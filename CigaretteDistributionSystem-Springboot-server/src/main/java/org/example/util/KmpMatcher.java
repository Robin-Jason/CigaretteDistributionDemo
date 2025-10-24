package org.example.util;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class KmpMatcher {
    
    /**
     * 使用KMP算法匹配字符串
     * @param text 母字符串
     * @param patterns 模式字符串列表
     * @return 匹配成功的模式字符串列表
     */
    public List<String> matchPatterns(String text, List<String> patterns) {
        List<String> matchedPatterns = new ArrayList<>();
        
        if (text == null || patterns == null || patterns.isEmpty()) {
            return matchedPatterns;
        }
        
        for (String pattern : patterns) {
            if (pattern != null && kmpSearch(text, pattern)) {
                matchedPatterns.add(pattern);
            }
        }
        
        return matchedPatterns;
    }
    
    /**
     * KMP搜索算法
     * @param text 文本字符串
     * @param pattern 模式字符串
     * @return 是否匹配成功
     */
    private boolean kmpSearch(String text, String pattern) {
        if (text == null || pattern == null || pattern.isEmpty()) {
            return false;
        }
        
        int[] lps = computeLPSArray(pattern);
        int i = 0; // text的索引
        int j = 0; // pattern的索引
        
        while (i < text.length()) {
            if (pattern.charAt(j) == text.charAt(i)) {
                i++;
                j++;
            }
            
            if (j == pattern.length()) {
                return true; // 找到匹配
            } else if (i < text.length() && pattern.charAt(j) != text.charAt(i)) {
                if (j != 0) {
                    j = lps[j - 1];
                } else {
                    i++;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 计算LPS（Longest Proper Prefix which is also Suffix）数组
     * @param pattern 模式字符串
     * @return LPS数组
     */
    private int[] computeLPSArray(String pattern) {
        int[] lps = new int[pattern.length()];
        int len = 0;
        int i = 1;
        
        lps[0] = 0; // lps[0]总是0
        
        while (i < pattern.length()) {
            if (pattern.charAt(i) == pattern.charAt(len)) {
                len++;
                lps[i] = len;
                i++;
            } else {
                if (len != 0) {
                    len = lps[len - 1];
                } else {
                    lps[i] = 0;
                    i++;
                }
            }
        }
        
        return lps;
    }
}
