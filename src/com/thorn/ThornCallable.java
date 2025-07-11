package com.thorn;

import java.util.List;

public interface ThornCallable {
    int arity();
    Object call(Interpreter interpreter, List<Object> arguments);
}