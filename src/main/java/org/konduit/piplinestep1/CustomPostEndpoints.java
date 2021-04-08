package org.konduit.piplinestep1;

import ai.konduit.serving.endpoint.Endpoint;
import ai.konduit.serving.endpoint.HttpEndpoints;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.impl.data.image.Jpeg;
import ai.konduit.serving.vertx.protocols.http.api.InferenceHttpApi;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import lombok.AllArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

public class CustomPostEndpoints implements HttpEndpoints {


    @Override
    public List<Endpoint> endpoints(Pipeline p, PipelineExecutor pe) {
        return Arrays.asList(new CustomPostEndpoint(),
                new CustomGetEndpoint(pe),
                new ImageEndpoint("data/test", "image", pe));
    }

    public static class CustomPostEndpoint implements Endpoint {
        private String PATH = "custom_post_endpoint";
        private Data input;
        private String output;

        @Override
        public HttpMethod type() {
            return HttpMethod.POST;
        }

        @Override
        public String path() {
            return PATH;
        }

        @Override
        public List<String> consumes() {
            return Collections.singletonList(APPLICATION_JSON.toString());
        }

        @Override
        public List<String> produces() {
            return Collections.singletonList(HttpHeaderValues.TEXT_PLAIN.toString());
        }

        @Override
        public Handler<RoutingContext> handler() {
            return ctx -> {
                input = InferenceHttpApi.extractData(APPLICATION_JSON.toString(), ctx);
                output = "Input as JSON: " + input.toJson();
                ctx.response()
                        .setStatusCode(200)
                        .putHeader(CONTENT_TYPE, TEXT_PLAIN.toString())
                        .end(output, StandardCharsets.UTF_8.name());
            };
        }
    }

    public static class  CustomGetEndpoint implements Endpoint {
        private final String PATH = "custom_get_endpoint";
        private final Data INPUT = Data.singleton("key", "value");;

        private Data output;

        private PipelineExecutor exec;
        public CustomGetEndpoint(PipelineExecutor exec){
            this.exec = exec;
        }

        @Override
        public HttpMethod type() {
            return HttpMethod.GET;
        }

        @Override
        public String path() {
            return PATH;
        }

        @Override
        public List<String> consumes() {
            return null;
        }

        @Override
        public List<String> produces() {
            return Collections.singletonList(APPLICATION_JSON.toString());
        }

        @Override
        public Handler<RoutingContext> handler() {
            return ctx -> {
                output = exec.exec(INPUT);

                ctx.response()
                        .setStatusCode(200)
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON.toString())
                        .end(output.toJson(), StandardCharsets.UTF_8.name());
            };
        }
    }
    @AllArgsConstructor
    public static class ImageEndpoint implements Endpoint {
        private final String endpoint;
        private final String imageName;
        private final PipelineExecutor exec;


        @Override
        public HttpMethod type() {
            return HttpMethod.GET;
        }

        @Override
        public String path() {
            return endpoint;
        }

        @Override
        public List<String> consumes() {
            //Null for GET
            return null;
        }

        @Override
        public List<String> produces() {
            return Collections.singletonList("image/jpeg");
        }

        @Override
        public Handler<RoutingContext> handler() {
            return rc -> {
                try {
                    Jpeg m = exec.exec(Data.empty()).getImage(imageName).getAs(Jpeg.class);

                    rc.response()
                            .setStatusCode(200)
                            .putHeader(CONTENT_TYPE, "image/jpeg")
                            .end(Buffer.buffer(Unpooled.wrappedBuffer(m.getFileBytes())));
                } catch (Throwable t) {
                    System.out.println("Error returning image" + t);
                    throw t;
                }
            };
        }
    }
}
