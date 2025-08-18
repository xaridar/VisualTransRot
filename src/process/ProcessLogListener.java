package process;

public interface ProcessLogListener {
    void onStdout(String line);
    void onStderr(String line);
}
