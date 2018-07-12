package com.agileengine.task;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    private static String targetElementId = "make-everything-ok-button";

    public static void main(String[] args) {
        Optional<Document> origDoc = findDocByPath(args.length > 0 ? args[0] : StringUtils.EMPTY);
        Optional<Document> similarDoc = findDocByPath(args.length > 1 ? args[1] : StringUtils.EMPTY);

        if (origDoc.isPresent() && similarDoc.isPresent()) {
            findAndPrintResult(origDoc.get(), similarDoc.get());
        } else {
            origDoc.orElseThrow(() -> new RuntimeException("Original file was not found!"));
            similarDoc.orElseThrow(() -> new RuntimeException("Similar file was not found!"));
        }
    }

    private static Optional<Document> findDocByPath(String path) {
        Optional<Document> result = Optional.empty();

        if (StringUtils.isNotBlank(path))
           result = findDocByFile(new File(path));

        return result;
    }

    private static Optional<Document> findDocByFile(File file) {
        Optional<Document> result = Optional.empty();

        try {
            result = Optional.ofNullable(Jsoup.parse(file, "utf8", file.getAbsolutePath()));
        } catch (IOException e) {
            System.out.println("Error reading file: " + file.getAbsolutePath());
        }

        return result;
    }

    private static void findAndPrintResult(Document origDoc, Document similarDoc) {
        Optional<Element> similarElement = findSimilarElement(origDoc, similarDoc);

        if (similarElement.isPresent())
            printResult(similarElement.get());
        else
            System.out.println("Similar element was not found");
    }

    private static void printResult(Element similarElement) {
        System.out.println("Similar element situated in the html tree:");
        List result = similarElement.parents().stream().map(Element::tagName).collect(Collectors.toList());
        Collections.reverse(result);
        result.forEach(x -> System.out.print(x + " > "));
        System.out.println("Element body:");
        System.out.println(similarElement.outerHtml());
    }

    private static Optional<Element> findSimilarElement(Document origDoc, Document similarDoc) {
        Optional<Element> result = Optional.empty();

        Optional<Element> origElement = findElementById(origDoc, targetElementId);

        if (origElement.isPresent())
            result = Stream.of(findElementById(similarDoc, targetElementId), findElementByAttributes(origElement.get(), similarDoc))
                    .filter(Optional::isPresent).map(Optional::get).max(getJaroWinklerDistanceComparator(buildStringFromAttributes(origElement)));
        else
            System.out.println("Original element was not found");

        return result;
    }

    private static Optional<Element> findElementByAttributes(Element origElement, Document similarDoc) {
        Comparator<Element> comparator = getJaroWinklerDistanceComparator(buildStringFromAttributes(Optional.of(origElement)));

        return origElement.attributes().asList().stream().
                filter(x -> StringUtils.isNotBlank(x.getKey()) && StringUtils.isNotBlank(x.getValue())).
                map(x -> similarDoc.getElementsByAttributeValue(x.getKey(), x.getValue()).stream().max(comparator)).
                filter(Optional::isPresent).map(Optional::get).max(comparator);
    }

    private static Comparator<Element> getJaroWinklerDistanceComparator(String origStringAttributes) {
        return Comparator.comparingDouble(o -> StringUtils.getJaroWinklerDistance(origStringAttributes, buildStringFromAttributes(Optional.of(o))));
    }

    private static String buildStringFromAttributes(Optional<Element> elementAttributes) {
        return elementAttributes.isPresent() ? elementAttributes.get().attributes().asList().stream()
                .map(x -> x.getKey() + " = " + x.getValue()).collect(Collectors.joining(", ")) : StringUtils.EMPTY;
    }

    private static Optional<Element> findElementById(Document doc, String elementId) {
       return Optional.ofNullable(doc.getElementById(targetElementId));
    }

}
