package actors;


import akka.actor.*;

public class ActorManagerExtension extends AbstractExtensionId<ActorManagerExtensionImpl> implements ExtensionIdProvider{

    @Override
    public ActorManagerExtensionImpl createExtension(ExtendedActorSystem system) {
        return new ActorManagerExtensionImpl(system);
    }

    public final static ActorManagerExtension ActorManagerExtensionProvider = new ActorManagerExtension();

    private ActorManagerExtension() {}

    @Override
    public ActorManagerExtension lookup() {
        return ActorManagerExtension.ActorManagerExtensionProvider;
    }
}
