package net.xavil.ultraviolet.client.screen.layer;

import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.collections.impl.Vector;

public final class Signal {

    public static enum CallbackAction {
        CONTINUE, ABORT;
    }

    public interface Callback {
        CallbackAction run();
    }

    public static final class Scope implements Disposable {

        private record Attachment(Signal signal, Runnable callback) {
        }

        private final Vector<Attachment> callbacks = new Vector<>();

        public void attach(Signal signal, Runnable callback) {
            this.callbacks.push(new Attachment(signal, callback));
        }

        @Override
        public void close() {
            this.callbacks.forEach(attachment -> {
                attachment.signal.detach(attachment.callback);
            });
        }

    }

    private final Vector<Runnable> callbacks = new Vector<>();

    public void fire() {
        this.callbacks.forEach(Runnable::run);
    }

    public void attach(Runnable callback) {
        this.callbacks.push(callback);
    }

    public void detach(Runnable callback) {
        final var index = this.callbacks.indexOf(callback);
        this.callbacks.swapRemove(index);
    }

}
