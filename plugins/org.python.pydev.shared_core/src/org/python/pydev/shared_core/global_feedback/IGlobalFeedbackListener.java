package org.python.pydev.shared_core.global_feedback;

public interface IGlobalFeedbackListener {

    void start(String msg);

    void progress(String msg);

    void end();
}
