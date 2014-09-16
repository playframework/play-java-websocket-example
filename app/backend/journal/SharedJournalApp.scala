/*
* Copyright © 2014 Typesafe, Inc. All rights reserved.
*/

package backend.journal

import akka.actor._
import akka.cluster.{Cluster, Member}
import akka.cluster.ClusterEvent.InitialStateAsEvents
import akka.persistence.journal.leveldb.{SharedLeveldbJournal, SharedLeveldbStore}
import scala.concurrent.duration.DurationInt
import akka.actor.RootActorPath
import akka.cluster.ClusterEvent.MemberUp
import akka.actor.ActorIdentity
import scala.Some
import akka.actor.Identify
import com.typesafe.config.ConfigFactory
import scala.io.StdIn

object SharedJournal {

    val name: String =
        "shared-journal"

    def pathFor(address: Address): ActorPath =
        RootActorPath(address) / "user" / name
}

/**
 * The shared journal is a single point of failure and must not be used in production.
 * This app must be running in order for persistence and cluster sharding to work.
 */
object SharedJournalApp {

    def main(args: Array[String]): Unit = {
        //improper / lazy checking - expecting port and cluster role.
        val system = if (args.size < 2)
            startSystem("0", "shared-journal")
        else
            startSystem(args(0), args(1))

        initialize(system)
        commandLoop(system)
    }

    def startSystem(port: String, role: String) = {
        val portNr = Integer.parseInt(port)

        // Override the port number configuration
        val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + portNr).
            withFallback(ConfigFactory.parseString(s"akka.cluster.roles=[$role]")).
            withFallback(ConfigFactory.parseString("akka.loglevel=INFO")).
            withFallback(ConfigFactory.load())

        // Create an actor system hello!
        ActorSystem("application", config)
    }

    def commandLoop(system: ActorSystem): Unit = {
        val line: String = StdIn.readLine()
        if (line.startsWith("s")) {
            system.shutdown()
        } else {
            commandLoop(system)
        }
    }

    def initialize(system: ActorSystem): Unit = {
        println(s"### SharedJournalApp initialize")
        val sharedJournal = system.actorOf(Props(new SharedLeveldbStore), SharedJournal.name)
        SharedLeveldbJournal.setStore(sharedJournal, system)
    }

}

object SharedJournalSetter {

    val name: String =
        "shared-journal-setter"

    def props: Props =
        Props(new SharedJournalSetter)
}

/**
 * This actor must be started and registered as a cluster event listener by all actor systems
 * that need to use the shared journal, e.g. in order to use persistence or cluster sharding.
 */
class SharedJournalSetter extends Actor with ActorLogging {

    override def preStart(): Unit =
        Cluster(context.system).subscribe(self, InitialStateAsEvents, classOf[MemberUp])

    override def receive: Receive =
        waiting

    private def waiting: Receive = {
        case MemberUp(member) if member hasRole SharedJournal.name => onSharedJournalMemberUp(member)
    }

    private def becomeIdentifying(): Unit = {
        context.setReceiveTimeout(10 seconds)
        context become identifying
    }

    private def identifying: Receive = {
        case ActorIdentity(_, Some(sharedJournal)) =>
            SharedLeveldbJournal.setStore(sharedJournal, context.system)
            log.info("### Successfully set shared journal {}", sharedJournal)
            context.stop(self)
        case ActorIdentity(_, None) =>
            log.error("### Can't identify shared journal!")
            context.stop(self)
        case ReceiveTimeout =>
            log.error("### Timeout identifying shared journal!")
            context.stop(self)
    }

    private def onSharedJournalMemberUp(member: Member): Unit = {
        val sharedJournal = context actorSelection SharedJournal.pathFor(member.address)
        log.info(s"### Identify of member address ${member.address}")
        sharedJournal ! Identify(None)
        becomeIdentifying()
    }
}
