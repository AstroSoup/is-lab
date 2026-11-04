package ru.astrosoup.islab.imports;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvParserTest {

    @Test
    void parsesSimpleRecordsWithLineNumbers() {
        List<CsvParser.Record> r = CsvParser.parse("a,b\n1,2\n3,4\n", ',');
        assertEquals(3, r.size());
        assertEquals(List.of("a", "b"), r.get(0).fields());
        assertEquals(2, r.get(1).line());
        assertEquals(List.of("3", "4"), r.get(2).fields());
    }

    @Test
    void handlesCrLfAndMissingFinalNewline() {
        List<CsvParser.Record> r = CsvParser.parse("a,b\r\n1,2", ',');
        assertEquals(2, r.size());
        assertEquals(List.of("1", "2"), r.get(1).fields());
    }

    @Test
    void handlesQuotesDelimitersAndNewlinesInsideFields() {
        List<CsvParser.Record> r = CsvParser.parse("\"a,b\",\"say \"\"hi\"\"\",\"x\ny\"\nnext,1,2\n", ',');
        assertEquals(2, r.size());
        assertEquals(List.of("a,b", "say \"hi\"", "x\ny"), r.get(0).fields());
        assertEquals(3, r.get(1).line(), "строка после многострочного значения считается правильно");
    }

    @Test
    void keepsEmptyTrailingAndQuotedEmptyFields() {
        List<CsvParser.Record> r = CsvParser.parse("a,,\"\",\n", ',');
        assertEquals(List.of("a", "", "", ""), r.get(0).fields());
    }

    @Test
    void rejectsUnclosedQuoteAndGarbageAfterQuote() {
        assertThrows(CsvParser.CsvFormatException.class, () -> CsvParser.parse("\"abc\n", ','));
        assertThrows(CsvParser.CsvFormatException.class, () -> CsvParser.parse("\"abc\"x,1\n", ','));
    }
}
