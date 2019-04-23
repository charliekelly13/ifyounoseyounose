package org.ifyounoseyounose.GUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.google.common.eventbus.Subscribe;

import java.io.File;

import javafx.scene.control.TextArea;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.richtext.TextExt;
import org.fxmisc.richtext.model.Codec;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.SegmentOps;
import org.fxmisc.richtext.model.StyledSegment;
import org.fxmisc.richtext.model.TextOps;
import org.ifyounoseyounose.backend.FileReport;
import org.ifyounoseyounose.backend.CompleteReport;
import org.reactfx.SuspendableNo;
import org.reactfx.util.Either;

public class Controller {

    @FXML private TextArea txtView;
    @FXML private TreeView<String> treeView;
    @FXML private Tab code;
    @FXML private MenuItem backToSetup;
    public String InputDirectory = null;//
    private Scene firstScene;
    private CompleteReport completeReport;
    private FileReport fileReport;
    private static Boolean JavaToggle;

    // the initialize method is automatically invoked by the FXMLLoader - it's magic
    public void initialize() {
        EventBusFactory.getEventBus().register(new Object() {
            @Subscribe
            public void setInputDirectory(EventBusFactory e) {
                InputDirectory = e.getFileLocation().replace("\\", "/");
                JavaToggle = e.getDisplayJava();
                displayTreeView(InputDirectory);
            }
        });

        code.setContent(displayCodeTab());

        treeView.getSelectionModel().selectedItemProperty().addListener((v, oldValue, newValue) -> {
            try {
                String classString = Files.readString(Path.of(getPathFromTreeView(v.getValue())));
                area.replaceText(classString);
                System.out.println("Oisin here - " + getPathFromTreeView(v.getValue()));
                fileReport = completeReport.getAllDetectedSmells(new File(getPathFromTreeView(v.getValue())));
                System.out.println("report thing: " + fileReport);
                setClassColours();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        //backToSetup.setOnAction(this::openFirstScene);//TODO this lets you go back , but doesn't clear everything

    }

    //this gets the filepath of a object from its treeview location
    public String getPathFromTreeView(TreeItem<String> v) {
        StringBuilder pathBuilder = new StringBuilder();
        for (TreeItem<String> item = v;
             item != null; item = item.getParent()) {

            pathBuilder.insert(0, item.getValue());
            pathBuilder.insert(0, "/");
        }
        return pathBuilder.toString().substring(1);
    }

    public void setCompleteReport(CompleteReport report) {
        completeReport = report;
    }

    public void setFirstScene(Scene scene) {
        firstScene = scene;
    }

    public void openFirstScene(ActionEvent actionEvent) {
        Stage primaryStage = (Stage) treeView.getScene().getWindow();
        primaryStage.setScene(firstScene);
    }

    public static void createTree(File file, TreeItem<String> parent) {
        if (file.isDirectory()) {
            TreeItem<String> treeItem = new TreeItem<>(file.getName());
            parent.getChildren().add(treeItem);
            for (File f : file.listFiles()) {
                createTree(f, treeItem);
            }
        } else if (!JavaToggle || file.getName().endsWith(".java")) {
            parent.getChildren().add(new TreeItem<>(file.getName()));
        }
    }

    public void displayTreeView(String inputDirectoryLocation) {
        TreeItem<String> rootItem = new TreeItem<>(inputDirectoryLocation);
        File Input = new File(inputDirectoryLocation);
        File fileList[] = Input.listFiles();

        // create tree
        for (File file : fileList) {
            createTree(file, rootItem);
        }
        treeView.setRoot(rootItem);
    }

    private final TextOps<String, TextStyle> styledTextOps = SegmentOps.styledTextOps();
    private final LinkedImageOps<TextStyle> linkedImageOps = new LinkedImageOps<>();
    private final GenericStyledArea<ParStyle, Either<String, LinkedImage>, TextStyle> area =
            new GenericStyledArea<>(
                    ParStyle.EMPTY,                                                 // default paragraph style
                    (paragraph, style) -> paragraph.setStyle(style.toCss()),        // paragraph style setter

                    TextStyle.EMPTY.updateFontSize(12).updateFontFamily("Serif").updateTextColor(Color.BLACK),  // default segment style
                    styledTextOps._or(linkedImageOps, (s1, s2) -> Optional.empty()),                            // segment operations
                    seg -> createNode(seg, (text, style) -> text.setStyle(style.toCss())));                     // Node creator and segment style setter

    {
        area.setWrapText(true);
        area.setStyleCodecs(
                ParStyle.CODEC,
                Codec.styledSegmentCodec(Codec.eitherCodec(Codec.STRING_CODEC, LinkedImage.codec()), TextStyle.CODEC));
    }

    private final SuspendableNo updatingToolbar = new SuspendableNo();


    public Node displayCodeTab() {
        area.setEditable(false);

        VirtualizedScrollPane<GenericStyledArea<ParStyle, Either<String, LinkedImage>, TextStyle>> vsPane = new VirtualizedScrollPane<>(area);
        VBox vbox = new VBox();
        VBox.setVgrow(vsPane, Priority.ALWAYS);
        vbox.getChildren().addAll(vsPane);

        Node node = vbox;
        return node;
    }

    private Node createNode(StyledSegment<Either<String, LinkedImage>, TextStyle> seg,
                            BiConsumer<? super TextExt, TextStyle> applyStyle) {
        return seg.getSegment().unify(
                text -> StyledTextArea.createStyledTextNode(text, seg.getStyle(), applyStyle),
                LinkedImage::createNode
        );
    }

    private void updateParagraphStyleInSelection(ParStyle mixin, int line) {
        setLineStyle(style -> style.updateWith(mixin), line);
    }

    private void updateParagraphBackground(Color color, int line) {
        if (!updatingToolbar.get()) {
            updateParagraphStyleInSelection(ParStyle.backgroundColor(color), line);
        }
    }

    public void setLineColour(Color color, int line) {//TODO Rename as set line smell
        if (line == 0) {
            updateParagraphBackground(color, line);//this is to set line 0
            for (int i = 1; i < area.getLength(); i++) {
                setLineColour(color, i);
            }
        } else {
            updateParagraphBackground(color, line);
        }
    }

    public void setClassColours() {
        HashMap<String, List<Integer>> fileReportHashMap = fileReport.getSmellDetections();
        Set<String> smellDetectors = fileReportHashMap.keySet();

        for (String s : smellDetectors) {
            //System.out.println(s);
            //List<Integer> smellyLines=fileReport.getSmellDetections(s);
            List<Integer> smellyLines = fileReportHashMap.get(s);
            for (int i : smellyLines) {
                if (i == 0) {
                    setLineColour(Color.BEIGE, i);
                } else {
                    setLineColour(Color.BEIGE, i - 1);
                }
            }
        }
    }

    private void setLineStyle(Function<ParStyle, ParStyle> updater, int line) {
        Paragraph<ParStyle, Either<String, LinkedImage>, TextStyle> paragraph = area.getParagraph(line);
        area.setParagraphStyle(line, updater.apply(paragraph.getParagraphStyle()));
    }
}