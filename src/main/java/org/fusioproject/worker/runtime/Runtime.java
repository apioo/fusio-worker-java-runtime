package org.fusioproject.worker.runtime;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.fusioproject.worker.runtime.exception.FileNotFoundException;
import org.fusioproject.worker.runtime.exception.InvalidActionException;
import org.fusioproject.worker.runtime.exception.RuntimeException;
import org.fusioproject.worker.runtime.generated.About;
import org.fusioproject.worker.runtime.generated.Execute;
import org.fusioproject.worker.runtime.generated.Response;
import org.fusioproject.worker.runtime.generated.ResponseHTTP;

import java.io.File;
import java.io.IOException;

public class Runtime {
    public About get() {
        About about = new About();
        about.setApiVersion("1.0.0");
        about.setLanguage("java");

        return about;
    }

    public Response run(File actionFile, Execute execute) throws RuntimeException {
        Connector connector = new Connector(execute.getConnections());
        Dispatcher dispatcher = new Dispatcher();
        Logger logger = new Logger();
        ResponseBuilder responseBuilder = new ResponseBuilder();

        Binding binding = new Binding();
        binding.setProperty("request", execute.getRequest());
        binding.setProperty("context", execute.getContext());
        binding.setProperty("connector", connector);
        binding.setProperty("response", responseBuilder);
        binding.setProperty("dispatcher", dispatcher);
        binding.setProperty("logger", logger);

        if (!actionFile.exists()) {
            throw new FileNotFoundException("Provided action files does not exist");
        }

        try {
            GroovyShell shell = new GroovyShell(binding);
            shell.evaluate(actionFile);
        } catch (IOException e) {
            throw new InvalidActionException("Could not execute action", e);
        }

        ResponseHTTP response = responseBuilder.getResponse();
        if (response == null) {
            response = new ResponseHTTP();
            response.setStatusCode(204);
        }

        Response result = new Response();
        result.setEvents(dispatcher.getEvents());
        result.setLogs(logger.getLogs());
        result.setResponse(response);

        return result;
    }
}
