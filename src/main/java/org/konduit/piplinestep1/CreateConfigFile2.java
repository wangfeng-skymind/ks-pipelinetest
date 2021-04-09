package org.konduit.piplinestep1;

import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.data.image.convert.config.AspectRatioHandling;
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
import java.util.Arrays;

public class CreateConfigFile2 {
    public static void main(String[] args) throws Exception {
        String filePath = CreateConfigFile1.class.getResource("/").getPath();

        filePath = filePath.substring(0, filePath.indexOf("target")) + "ks/";
        Pipeline p = getPipeline(false, false);

        InferenceConfiguration conf = new InferenceConfiguration()
                .pipeline(p)
                .port(9018)
                .protocol(ServerProtocol.HTTP);
                //.customEndpoints(Collections.singletonList(CustomPostEndpoints.class.getName()));
        String yaml = conf.toYaml();
        File outYaml = new File(filePath + "piplinestep1.yaml");
        FileUtils.writeStringToFile(outYaml, yaml, StandardCharsets.UTF_8);
    }

    public static Pipeline getPipeline(boolean addShowSteps, boolean resizeAndCrop) {
        ImageToNDArrayConfig cf = new ImageToNDArrayConfig()
                .height(28) // This will resize the image height already (no need for the ImageResizeStep)
                .width(28) // This will resize the image width already (no need for the ImageResizeStep)
                .channelLayout(NDChannelLayout.GRAYSCALE) // This will only contain a single channel (for gray scale)
                .format(NDFormat.CHANNELS_FIRST) // This will keep the channel dimension first. For example, [1, 28, 28] with 'CHANNEL_FIRST' as compared to [28, 28, 1] with 'CHANNEL_LAST'
                .includeMinibatchDim(true) // This will include an extra dimension in the final array. For example, it will look like [1, 1, 28, 28] with 'true' as compared to [1, 28, 28] with 'false'.
                .dataType(NDArrayType.FLOAT) // The output array will contain values in 'float' data type.
                .aspectRatioHandling(AspectRatioHandling.CENTER_CROP); // This will crop the image in the center to make sure that the image isn't distorted due to resize operation.

        GraphBuilder b = new GraphBuilder();
        GraphStep input = b.input();

        if(!resizeAndCrop) {
            input = input.then("data", new ImageToNDArrayStep()
                    .config(cf) // Passed in the image configuration
                    .keys("imagesd") // The input image will be given in the key. For example, { "imagesd": <IMAGE> }
                    .keepOtherValues(true) // This will ignore the other incoming values from the previous step. It's better to keep this as true since we don't want to lose data between pipeline steps.
                    .metadata(false) // False by default. If true, include metadata about the images in the output data. For example, if/how it was cropped, and the original input size.
                    .metadataKey("@ImageToNDArrayStepMetadata") // "Sets the key that the metadata will be stored under. Not relevant if metadata == false."
                    .outputNames("data") // "May be null. If non-null, the input images are renamed to this in the output Data instance after conversion to n-dimensional array."
            );
        } else {
            input = input.then("resize",
                    new ImageResizeStep()
                            .height(25)
                            .width(25)
                            .aspectRatioHandling(AspectRatioHandling.PAD)
                            .inputNames("imagesd") // The output will be with the same key which will contain the resized image
            );
            input = input.then("crop",
                    new ImageCropStep()
                            .imageName("imagesd") // Name of the incoming image, this will be the output key of the image as well.
                            .coordsArePixels(true) // "Whether the crop region (BoundingBox / List<Point> are specified in pixels, or 'fraction of image'"
                            .cropPoints(Arrays.asList(
                                    Point.create(10, 10), // The top left crop point
                                    Point.create(20, 20)) // The bottom right crop point
                            )
            );

            if (addShowSteps) {
                input = input.then("show",
                        new ShowImageStep()
                                .imageName("imagesd")); // This key should be equal to the same key that's coming from the previous step. You can't assign a random string here and expect it to find the image.
            }
        }
        input = input.then("log",
                new LoggingStep().logLevel(Level.INFO)
                .log(LoggingStep.Log.KEYS_AND_VALUES));

        return b.build(input);
    }
}
