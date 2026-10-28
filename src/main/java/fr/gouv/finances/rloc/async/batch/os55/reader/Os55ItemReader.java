package fr.gouv.finances.rloc.async.batch.os55.reader;

import java.nio.file.Path;

import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.core.io.FileSystemResource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.gouv.finances.rloc.async.rest.os55.dto.Os55DemandeDTO;

public class Os55ItemReader extends FlatFileItemReader<Os55DemandeDTO> {

    public Os55ItemReader(Path inputFile, ObjectMapper objectMapper) {
        setName("os55ItemReader");
        setResource(new FileSystemResource(inputFile));
        setSaveState(true);
        setLineMapper((line, lineNumber) -> {
            try {
                return objectMapper.readValue(line, Os55DemandeDTO.class);
            } catch (JsonProcessingException exception) {
                throw new FlatFileParseException("Ligne JSONL OS55 malformee", exception, line, lineNumber);
            }
        });
    }
}
