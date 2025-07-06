package com.thorn;

import java.util.List;

class ThornFunction implements ThornCallable {
    private final String name;
    private final List<Token> params;
    private final List<Stmt> body;
    private final Environment closure;

    ThornFunction(String name, List<Token> params, List<Stmt> body, Environment closure) {
        this.name = name;
        this.params = params;
        this.body = body;
        this.closure = closure;
    }

    @Override
    public int arity() {
        return params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);
        for (int i = 0; i < params.size(); i++) {
            environment.define(params.get(i).lexeme, arguments.get(i), false);
        }

        try {
            interpreter.executeBlock(body, environment);
        } catch (Interpreter.Return returnValue) {
            return returnValue.value;
        }
        return null;
    }

    ThornFunction bind(ThornInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance, false);
        return new ThornFunction(name, params, body, environment);
    }

    @Override
    public String toString() {
        if (name == null) return "<lambda>";
        return "<fn " + name + ">";
    }
}