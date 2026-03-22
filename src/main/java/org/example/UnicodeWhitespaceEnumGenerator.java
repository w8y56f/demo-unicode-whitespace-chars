package org.example;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * 不可见空白类Unicode字符枚举管理工具
 * 优化点：description列固定30个字符（不足补空格，超长截断），保证所有列对齐
 */
public class UnicodeWhitespaceEnumGenerator {

    public enum WhitespaceUnicode {
        // 1. 基础空格与空白类 (Spaces)
        U_0020("普通空格（Space）", "U+0020", "\\u0020", "\u0020"),
        U_00A0("不换行空格（NBSP）", "U+00A0", "\\u00A0", "\u00A0"),
        U_2002("半角空格（ENSP）", "U+2002", "\\u2002", "\u2002"),
        U_2003("全角空格（EMSP）", "U+2003", "\\u2003", "\u2003"),
        U_2009("细空格（Thin Space）", "U+2009", "\\u2009", "\u2009"),
        U_202F("窄不换行空格（NNBSP）", "U+202F", "\\u202F", "\u202F"),
        U_3000("CJK全角空格", "U+3000", "\\u3000", "\u3000"),

        // 2. 零宽字符类 (Zero Width - 极度隐蔽，最易导致数据库查询失败)
        U_200B("零宽空格（ZWSP）", "U+200B", "\\u200B", "\u200B"),
        U_200C("零宽不连字符（ZWNJ）", "U+200C", "\\u200C", "\u200C"),
        U_200D("零宽连字符（ZWJ）", "U+200D", "\\u200D", "\u200D"),
        U_FEFF("零宽不换行空格（BOM/ZWNBSP）", "U+FEFF", "\\uFEFF", "\uFEFF"),
        U_2060("词连字符（Word Joiner）", "U+2060", "\\u2060", "\u2060"),

        // 3. 危险控制字符类 (Control Codes - 可能导致 SQL 截断或解析崩溃)
        U_0000("空字符（NULL）- 极其危险，易截断SQL", "U+0000", "\\u0000", "\0"),
        U_0007("响铃符（BEL）", "U+0007", "\\u0007", "\u0007"),
        U_0008("退格符（BS）", "U+0008", "\\u0008", "\b"),
        U_0009("水平制表符（Tab）", "U+0009", "\\u0009", "\t"),
        U_000A("换行符（LF）", "U+000A", "\\u000A", "\n"),
        U_000C("换页符（FF）", "U+000C", "\\u000C", "\f"),
        U_000D("回车符（CR）", "U+000D", "\\u000D", "\r"),
        U_001B("转义符（ESC）", "U+001B", "\\u001B", "\u001B"),
        U_007F("删除符（DEL）", "U+007F", "\\u007F", "\u007F"),
        U_0085("下一行（NEL）", "U+0085", "\\u0085", "\u0085"),

        // 4. 行/段分隔符
        U_2028("行分隔符（LSEP）", "U+2028", "\\u2028", "\u2028"),
        U_2029("段落分隔符（PSEP）", "U+2029", "\\u2029", "\u2029"),

        // 5. 双向控制符 (Bidi Controls - 导致视觉显示与存储内容顺序不符)
        U_200E("左到右标志（LRM）", "U+200E", "\\u200E", "\u200E"),
        U_200F("右到左标志（RLM）", "U+200F", "\\u200F", "\u200F"),
        U_202A("左到右嵌入（LRE）", "U+202A", "\\u202A", "\u202A"),
        U_202B("右到左嵌入（RLE）", "U+202B", "\\u202B", "\u202B"),
        U_202C("方向重置（PDF）", "U+202C", "\\u202C", "\u202C"),
        U_202D("左到右覆盖（LRO）", "U+202D", "\\u202D", "\u202D"),
        U_202E("右到左覆盖（RLO）", "U+202E", "\\u202E", "\u202E"),
        U_2066("左到右隔离（LRI）", "U+2066", "\\u2066", "\u2066"),
        U_2067("右到左隔离（RLI）", "U+2067", "\\u2067", "\u2067"),
        U_2069("隔离重置（PDI）", "U+2069", "\\u2069", "\u2069"),

        // 6. 其他隐形功能符
        U_00AD("软连字符（SHY）- 仅换行显示，平时不可见", "U+00AD", "\\u00AD", "\u00AD"),
        U_2061("函数应用符", "U+2061", "\\u2061", "\u2061"),
        U_2062("隐形乘号", "U+2062", "\\u2062", "\u2062"),
        U_2063("隐形分隔符", "U+2063", "\\u2063", "\u2063");

        private final String description;
        private final String unicodeLabel;
        private final String javaEscape;
        private final String actualChar;

        WhitespaceUnicode(String description, String unicodeLabel, String javaEscape, String actualChar) {
            this.description = description;
            this.unicodeLabel = unicodeLabel;
            this.javaEscape = javaEscape;
            this.actualChar = actualChar;
        }

        public String getDescription() { return description; }
        public String getUnicodeLabel() { return unicodeLabel; }
        public String getJavaEscape() { return javaEscape; }
        public String getActualChar() { return actualChar; }
    }

    /**
     * 字符串定长填充工具方法
     * @param str 原字符串
     * @param length 目标长度（字符数，中文/英文均算1个字符）
     * @return 固定长度字符串：不足补空格，超长则截断
     */
    private static String padString(String str, int length) {
        if (str == null) {
            str = "";
        }
        // 1. 超长则截断
        if (str.length() > length) {
            return str.substring(0, length);
        }
        // 2. 不足则补空格（用StringBuilder保证效率）
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < length) {
            sb.append(" "); // 补普通空格（U+0020）
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        String filePath = "unicode_whitespace_enum.txt";
        // 定义description固定长度（可根据需要调整）
        final int DESC_FIXED_LENGTH = 30;

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8)
        )) {
            // 写入文件头（对应固定长度的description）
            writer.write("=== 不可见空白类Unicode字符枚举汇总 ===\n");
            writer.write(String.format(
                    "%s\t%-8s\t%-10s\t实际字符（启用特殊字符显示可见）\n",
                    "枚举名",
                    padString("描述", DESC_FIXED_LENGTH),
                    "U+标识",
                    "Java转义写法"
            ));
            writer.write("----------------------------------------------------------------------------------------------------\n");

            // 遍历枚举，写入所有字符信息（列对齐）
            for (WhitespaceUnicode enumItem : WhitespaceUnicode.values()) {
                // 对description做固定长度处理（30字符）
                String fixedDesc = padString(enumItem.getDescription(), DESC_FIXED_LENGTH);
                writer.write(String.format(
                        "%s\t%-8s\t%-10s\t%s\n",
                        //"%s\t%-8s\t%-10s\t'%s'\n",
                        fixedDesc,               // 固定30字符的描述
                        enumItem.getUnicodeLabel(), // U+标识（左对齐，占8字符）
                        enumItem.getJavaEscape(),   // Java转义写法（左对齐，占10字符）
                        enumItem.getActualChar()    // 实际字符
                ));
            }

            System.out.println("文件生成成功！路径：" + System.getProperty("user.dir") + "/" + filePath);
            System.out.println("提示：用DBeaver/Notepad++打开，启用「显示特殊字符」可查看实际字符的可视化标记");
        } catch (Exception e) {
            System.err.println("文件生成失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
}