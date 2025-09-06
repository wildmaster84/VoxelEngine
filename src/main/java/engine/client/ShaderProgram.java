package engine.client;

import org.lwjgl.opengl.GL20;

import java.nio.file.Files;
import java.nio.file.Paths;

public class ShaderProgram {
    public int programId;

    public ShaderProgram(String vertPath, String fragPath) throws Exception {
        int vertId = createShader(vertPath, GL20.GL_VERTEX_SHADER);
        int fragId = createShader(fragPath, GL20.GL_FRAGMENT_SHADER);
        programId = GL20.glCreateProgram();
        GL20.glAttachShader(programId, vertId);
        GL20.glAttachShader(programId, fragId);
        GL20.glLinkProgram(programId);
        if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == 0)
            throw new RuntimeException("Shader link failed: " + GL20.glGetProgramInfoLog(programId));
        GL20.glDetachShader(programId, vertId);
        GL20.glDetachShader(programId, fragId);
        GL20.glDeleteShader(vertId);
        GL20.glDeleteShader(fragId);
    }

    private int createShader(String path, int type) throws Exception {
        String src = new String(Files.readAllBytes(Paths.get(path)));
        int id = GL20.glCreateShader(type);
        GL20.glShaderSource(id, src);
        GL20.glCompileShader(id);
        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == 0)
            throw new RuntimeException("Shader compile failed: " + GL20.glGetShaderInfoLog(id));
        return id;
    }

    public void use() {
        GL20.glUseProgram(programId);
    }

    public void stop() {
        GL20.glUseProgram(0);
    }

    public void setUniform(String name, int value) {
        int loc = GL20.glGetUniformLocation(programId, name);
        GL20.glUniform1i(loc, value);
    }
}