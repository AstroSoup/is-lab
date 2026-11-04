package ru.astrosoup.islab.imports;

import org.junit.jupiter.api.Test;
import ru.astrosoup.islab.entities.OrganizationType;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class OrganizationCsvParserTest {

    private static final String HEADER =
            "name,fullName,coordinatesX,coordinatesY,annualTurnover,employeesCount,rating,type,officialAddress,postalAddress\n";

    private static OrganizationCsvParser.ParseResult parse(String csv) {
        return OrganizationCsvParser.parse(csv.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void parsesRowWithNestedObjectsInSameRecord() {
        var result = parse(HEADER + "Acme,Acme Inc.,10.5,20,1000,50,3,commercial,Main st 1,\"Post st, 2\"\n");
        assertTrue(result.errors().isEmpty(), result.errors().toList().toString());
        assertEquals(1, result.rows().size());
        ImportRow row = result.rows().get(0);
        assertEquals("Acme", row.name());
        assertEquals("Acme Inc.", row.fullName());
        assertEquals(10.5, row.x());
        assertEquals(20, row.y());
        assertEquals(1000L, row.annualTurnover());
        assertEquals(50, row.employeesCount());
        assertEquals(3, row.rating());
        assertEquals(OrganizationType.COMMERCIAL, row.type());
        assertEquals("Main st 1", row.officialStreet());
        assertEquals("Post st, 2", row.postalStreet());
        assertEquals(2, row.line());
    }

    @Test
    void optionalColumnsMayBeAbsentOrEmpty() {
        var result = parse("rating,name,employeesCount,annualTurnover,coordinatesY,coordinatesX\n3,Acme,5,100,1,2\n");
        assertTrue(result.errors().isEmpty(), result.errors().toList().toString());
        ImportRow row = result.rows().get(0);
        assertNull(row.fullName());
        assertNull(row.type());
        assertNull(row.officialStreet());
        assertNull(row.postalStreet());
        assertEquals(2.0, row.x());
    }

    @Test
    void acceptsSemicolonDelimiterAndBom() {
        String csv = "﻿" + HEADER.replace(',', ';') + "Acme;;1;2;3;4;5;;;\n";
        var result = parse(csv);
        assertTrue(result.errors().isEmpty(), result.errors().toList().toString());
        assertEquals(1, result.rows().size());
    }

    @Test
    void reportsAllRowErrorsWithLineNumbers() {
        var result = parse(HEADER
                + "A,,1,2,3,4,5,,,\n"
                + "B,,abc,2,3,4,5,,,\n"
                + "C,,1,2,3.5,4,5,,,\n"
                + "D,,1,2,3,4,,,,\n"
                + "E,,1,2,3,4,5,wrong,,\n");
        assertEquals(1, result.rows().size());
        var errors = result.errors().toList();
        assertEquals(4, errors.size(), errors.toString());
        assertTrue(errors.get(0).startsWith("строка 3:"), errors.get(0));
        assertTrue(errors.get(1).startsWith("строка 4:"), errors.get(1));
        assertTrue(errors.get(2).startsWith("строка 5:"), errors.get(2));
        assertTrue(errors.get(3).startsWith("строка 6:"), errors.get(3));
    }

    @Test
    void rejectsIntegerOverflowAndNonFiniteNumbers() {
        var result = parse(HEADER
                + "A,,1e999,2,3,4,5,,,\n"
                + "B,,1,99999999999,3,4,5,,,\n"
                + "C,,NaN,2,3,4,5,,,\n");
        assertEquals(0, result.rows().size());
        assertEquals(3, result.errors().total());
    }

    @Test
    void rejectsBadHeader() {
        assertFalse(parse("name,coordinatesX\nA,1\n").errors().isEmpty());
        assertFalse(parse(HEADER.replace("rating", "ratng") + "A,,1,2,3,4,5,,,\n").errors().isEmpty());
        assertFalse(parse(HEADER.trim() + ",name\nA,,1,2,3,4,5,,,,A\n").errors().isEmpty());
    }

    @Test
    void rejectsWrongColumnCount() {
        var result = parse(HEADER + "A,1,2\n");
        assertEquals(1, result.errors().total());
        assertTrue(result.errors().toList().get(0).startsWith("строка 2:"));
    }

    @Test
    void rejectsEmptyHeaderOnlyAndNonUtf8Files() {
        assertFalse(OrganizationCsvParser.parse(new byte[0]).errors().isEmpty());
        assertFalse(parse(HEADER).errors().isEmpty());
        byte[] cp1251 = (HEADER + "Организация,,1,2,3,4,5,,,\n").getBytes(Charset.forName("windows-1251"));
        assertFalse(OrganizationCsvParser.parse(cp1251).errors().isEmpty());
    }

    @Test
    void blankNameStaysNullSoDomainValidationRejectsIt() {
        var result = parse(HEADER + "   ,,1,2,3,4,5,,,\n");
        assertTrue(result.errors().isEmpty());
        assertNull(result.rows().get(0).name());
    }

    @Test
    void errorLogIsCapped() {
        StringBuilder sb = new StringBuilder(HEADER);
        for (int i = 0; i < 150; i++) sb.append("A,,x,2,3,4,5,,,\n");
        var result = parse(sb.toString());
        assertEquals(150, result.errors().total());
        assertEquals(ErrorLog.LIMIT + 1, result.errors().toList().size());
    }
}
