package com.tdtech.cloudcmd.util;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.dictionary.CustomDictionary;
import com.hankcs.hanlp.seg.Dijkstra.DijkstraSegment;
import com.hankcs.hanlp.seg.NShort.NShortSegment;
import com.hankcs.hanlp.seg.Segment;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class LPUtils{

    private static final Segment N_SHORT_SEGMENT = new NShortSegment().enableCustomDictionary(true)
        .enablePlaceRecognize(true).enableOrganizationRecognize(true).enableNameRecognize(true);
    private static final Segment SHORTEST_SEGMENT = new DijkstraSegment().enableCustomDictionary(true)
        .enablePlaceRecognize(true).enableOrganizationRecognize(true).enableNameRecognize(true);

    /**
     * 普通分词 最快
     */
    public String segment(String source) {
        return Optional.ofNullable(HanLP.segment(source)).stream().flatMap(Collection::stream).map(a -> a.word)
            .collect(Collectors.joining(" "));
    }

    /**
     * n最短分词 效果较好，时间较慢，一般用最短分词
     */
    public String nShortSegment(String source) {
        return Optional.ofNullable(N_SHORT_SEGMENT.seg(source)).stream().flatMap(Collection::stream).map(a -> a.word)
            .collect(Collectors.joining(" "));
    }

    /**
     * 最短分词
     */
    public static String shortestSegment(String source) {
        CustomDictionary.add("看板");
        return Optional.ofNullable(SHORTEST_SEGMENT.seg(source)).stream().flatMap(Collection::stream).map(a -> a.word)
            .collect(Collectors.joining(" "));
    }

    public static void main(String[] args) {
        String s = "组织话务统计Organize traffic statistics";
        System.out.println(shortestSegment(s));
    }
}
