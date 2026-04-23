package com.example.server.imports.parser;

import java.util.List;

public record PdfParseResult(
    List<ParsedCourseRecord> records,
    List<ParserWarning> warnings
) {}
