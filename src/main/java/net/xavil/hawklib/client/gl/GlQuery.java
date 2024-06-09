package net.xavil.hawklib.client.gl;

import org.lwjgl.opengl.GL45C;

public final class GlQuery extends GlObject {

    public static enum Type {
        SAMPLES_PASSED(GL45C.GL_SAMPLES_PASSED),
        ANY_SAMPLES_PASSED(GL45C.GL_ANY_SAMPLES_PASSED),
        ANY_SAMPLES_PASSED_CONSERVATIVE(GL45C.GL_ANY_SAMPLES_PASSED_CONSERVATIVE),
        TIME_ELAPSED(GL45C.GL_TIME_ELAPSED),
        TIMESTAMP(GL45C.GL_TIMESTAMP),
        PRIMITIVES_GENERATED(GL45C.GL_PRIMITIVES_GENERATED),
        TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN(GL45C.GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN);

        public final int id;

        private Type(int id) {
            this.id = id;
        }
    }

    private final Type type;

    public GlQuery(Type type, int id, boolean owned) {
        super(ObjectType.QUERY, id, owned);
        this.type = type;
    }

    public GlQuery(Type type) {
        this(type, GL45C.glCreateQueries(type.id), true);
    }

    public void writeTimestamp() {
        if (this.type != Type.TIMESTAMP)
            throw new IllegalStateException(String.format(
                    "Cannot write timestamp, query type is %s",
                    this.type));
        GL45C.glQueryCounter(this.id, Type.TIMESTAMP.id);
    }

    public boolean isResultAvailable() {
        return GL45C.glGetQueryObjecti(this.id, GL45C.GL_QUERY_RESULT_AVAILABLE) != GL45C.GL_FALSE;
    }

    public long clientWaitValue() {
        return GL45C.glGetQueryObjecti64(this.id, GL45C.GL_QUERY_RESULT);
    }

}
