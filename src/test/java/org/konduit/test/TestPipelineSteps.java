package org.konduit.test;

import ai.konduit.serving.vertx.api.DeployKonduitServing;
import ai.konduit.serving.vertx.config.InferenceConfiguration;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.VertxOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.konduit.piplinestep1.CreateConfigFile1;
import org.konduit.piplinestep1.CreateConfigFile2;
import org.nd4j.common.io.ClassPathResource;

import java.io.IOException;

@RunWith(VertxUnitRunner.class)
public class TestPipelineSteps {

    @Test
    public void testCustomStep(TestContext testContext) {
        Async async = testContext.async();

        DeployKonduitServing.deploy(
                new VertxOptions(),
                new DeploymentOptions(),
                new InferenceConfiguration().pipeline(
                        CreateConfigFile2.getPipeline(true, true)
                ),
                handler -> {
                    if (handler.succeeded()) {
                        try {
                            System.out.println(
                                    Unirest.post(String.format("http://localhost:%s/predict", handler.result().getActualPort()))
                                            .header("Accept", "application/json")
                                            .field("imagesd", new ClassPathResource("5.png").getFile(), "image/png")
                                            .asString().getBody()
                            );
                        } catch (IOException | UnirestException e) {
                            testContext.fail(e);
                        }
                        async.complete();
                    } else {
                        testContext.fail(handler.cause());
                    }
                }
        );
    }

}
