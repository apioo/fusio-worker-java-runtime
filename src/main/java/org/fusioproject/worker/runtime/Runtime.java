package org.fusioproject.worker.runtime;

import groovy.lang.GroovyShell;
import groovy.lang.MissingMethodException;
import groovy.lang.Script;
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

        Object[] arguments = {
            execute.getRequest(),
            execute.getContext(),
            connector,
            responseBuilder,
            dispatcher,
            logger,
        };

        if (!actionFile.exists()) {
            throw new FileNotFoundException("Provided action files does not exist");
        }

        ResponseHTTP response;

        try {
            GroovyShell shell = new GroovyShell();
            Script script = shell.parse(actionFile);

            Object result = script.invokeMethod("handle", arguments);

            if (result instanceof ResponseHTTP) {
                response = (ResponseHTTP) result;
            } else {
                response = new ResponseHTTP();
                response.setStatusCode(204);
            }
        } catch (MissingMethodException e) {
            throw new InvalidActionException("Script does not contain a handle method", e);
        } catch (IOException e) {
            throw new InvalidActionException("Could not execute action", e);
        }

        Response result = new Response();
        result.setEvents(dispatcher.getEvents());
        result.setLogs(logger.getLogs());
        result.setResponse(response);

        return result;
    }
}
