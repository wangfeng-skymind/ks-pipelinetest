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

public class CreateConfigFile3 {
    public static void main(String[] args) throws Exception {
        String filePath = CreateConfigFile1.class.getResource("/").getPath();

        filePath = filePath.substring(0, filePath.indexOf("target")) + "ks/";
        Pipeline p = getPipeline(false, false);

        InferenceConfiguration conf = new InferenceConfiguration()
                .pipeline(p)
                .port(9018)
                .protocol(ServerProtocol.HTTP);
        String yaml = conf.toYaml();
        File outYaml = new File(filePath + "piplinestep1.yaml");
        FileUtils.writeStringToFile(outYaml, yaml, StandardCharsets.UTF_8);
    }

    public static Pipeline getPipeline(boolean addShowSteps, boolean resizeAndCrop) {
        ImageToNDArrayConfig cf = new ImageToNDArrayConfig()
                .height(28) // 这将调整图像高度（不需要ImageResizeStep）
                .width(28) // 这将调整图像宽度（不需要ImageResizeStep）
                .channelLayout(NDChannelLayout.GRAYSCALE) // 这将只包含一个通道（用于灰度）
                .format(NDFormat.CHANNELS_FIRST) // 这将首先保持通道尺寸。例如，[1，28，28]与'CHANNEL FIRST'比较，而[28，28，1]与'CHANNEL LAST'
                .includeMinibatchDim(true) // 这将在最终数组中包含一个额外维度。例如，与带有“false”的[1，28，28]相比，它看起来像带有“true”的[1，1，28，28]。
                .dataType(NDArrayType.FLOAT) // 输出数组将包含“float”数据类型的值。
                .aspectRatioHandling(AspectRatioHandling.CENTER_CROP); // 这将在中心裁剪图像，以确保图像不会因调整大小操作而扭曲。

        GraphBuilder b = new GraphBuilder();
        GraphStep input = b.input();

        if(!resizeAndCrop) {
            input = input.then("data", new ImageToNDArrayStep()
                    .config(cf) // 传入映像配置
                    .keys("imagesd") // 输入图像将在键中给出。例如，{“imagesd”：<IMAGE>}
                    .keepOtherValues(true) // 这将忽略上一步中的其他传入值。最好保持这一点，因为我们不希望在管道步骤之间丢失数据。
                    .metadata(false) // 默认为False。如果为true，则在输出数据中包含有关图像的元数据。例如，如果/如何裁剪，以及原始输入大小。
                    .metadataKey("@ImageToNDArrayStepMetadata") // “设置元数据将在其下存储的密钥。如果元数据==false，则不相关。“
                    .outputNames("data") // “可能为空。如果为非空，则在转换为n维数组后，在输出数据实例中将输入图像重命名为该值。“
            );
        } else {
            input = input.then("resize",
                    new ImageResizeStep()
                            .height(25)
                            .width(25)
                            .aspectRatioHandling(AspectRatioHandling.PAD)
                            .inputNames("imagesd") //输出将与包含已调整大小的图像的键相同
            );

            input = input.then("crop",
                    new ImageCropStep()
                            .imageName("imagesd") // 传入图像的名称，这也将是图像的输出键。
                            .coordsArePixels(true) // 裁剪区域（BoundingBox/List<Point>是以像素表示，还是以“图像分数”表示
                            .cropPoints(Arrays.asList(
                                    Point.create(10, 10), // 左上裁剪点
                                    Point.create(20, 20)) //右下裁剪点
                            )
            );

            if (addShowSteps) {
                input = input.then("show",
                        new ShowImageStep()
                                .imageName("imagesd")); // 此键应与上一步中的键相同。您不能在这里分配一个随机字符串，并期望它找到图像。
            }
        }

        input = input.then("log",
                new LoggingStep().logLevel(Level.INFO)
                        .log(LoggingStep.Log.KEYS_AND_VALUES));

        return b.build(input);
    }
}
