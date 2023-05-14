package eu.pkgsoftware.babybuddywidgets.utils;

public interface Promise<S, F> {
    void succeeded(S s);

    void failed(F f);
}

