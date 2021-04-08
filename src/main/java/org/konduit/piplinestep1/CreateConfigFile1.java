package org.konduit.piplinestep1;



import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.data.image.convert.config.AspectRatioHandling;
import ai.konduit.serving.data.image.convert.config.ImageNormalization;
import ai.konduit.serving.data.image.convert.config.NDChannelLayout;
import ai.konduit.serving.data.image.convert.config.NDFormat;
import ai.konduit.serving.data.image.step.crop.ImageCropStep;
import ai.konduit.serving.data.image.step.ndarray.ImageToNDArrayStep;
import ai.konduit.serving.data.image.step.resize.ImageResizeStep;
import ai.konduit.serving.data.image.step.show.ShowImageStep;
import ai.konduit.serving.pipeline.api.data.NDArrayType;
import ai.konduit.serving.pipeline.api.data.Point;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.impl.pipeline.graph.GraphBuilder;
import ai.konduit.serving.pipeline.impl.pipeline.graph.GraphStep;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingStep;
import ai.konduit.serving.vertx.config.InferenceConfiguration;
import ai.konduit.serving.vertx.config.ServerProtocol;
import org.apache.commons.io.FileUtils;
import org.slf4j.event.Level;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class CreateConfigFile1 {
    public static void main(String[] args) throws Exception {
        String filePath = CreateConfigFile1.class.getResource("/").getPath();

        filePath = filePath.substring(0, filePath.indexOf("target")) + "ks/";
        Pipeline p = getPipeline(false);

        InferenceConfiguration conf = new InferenceConfiguration()
                .pipeline(p)
                .port(9018)
                .protocol(ServerProtocol.HTTP);
                //.customEndpoints(Collections.singletonList(CustomPostEndpoints.class.getName()));
        String yaml = conf.toYaml();
        File outYaml = new File(filePath + "piplinestep1.yaml");
        FileUtils.writeStringToFile(outYaml, yaml, StandardCharsets.UTF_8);
    }

    public static Pipeline getPipeline(boolean addShowSteps) {


        ImageToNDArrayConfig cf = new ImageToNDArrayConfig()
                .height(28)
                .width(28)
                .channelLayout(NDChannelLayout.GRAYSCALE)
                .includeMinibatchDim(true)
                .format(NDFormat.CHANNELS_FIRST)
                .dataType(NDArrayType.FLOAT)
                .aspectRatioHandling(AspectRatioHandling.CENTER_CROP);

        GraphBuilder b = new GraphBuilder();
        GraphStep input = b.input();
        input = input.then("data", new ImageToNDArrayStep().config(cf).keys("imagesd")
                .keepOtherValues(false).metadata(true).metadataKey("@ImageToNDArrayStepMetadata").outputNames("data"));

        input = input.then("resize", new ImageResizeStep().height(25).width(25)
                .aspectRatioHandling(AspectRatioHandling.PAD).inputNames("data"));

        input = input.then("crop", new ImageCropStep().coordsArePixels(true)
                .cropPoints(Collections.singletonList(Point.create(1,1))));

        input = input.then("show", new ShowImageStep().imageName("reading:gray"));

        input = input.then("log", new LoggingStep().logLevel(Level.INFO)
                .log(LoggingStep.Log.KEYS_AND_VALUES));
        return b.build(input);
    }
}
