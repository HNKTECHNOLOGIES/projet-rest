package fr.gouv.finances.rloc.async.batch.os55.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileParseException;

import com.fasterxml.jackson.databind.ObjectMapper;

class Os55ItemReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void readsJsonlLineByLine() throws Exception {
        Path file = tempDir.resolve("input.ndjson");
        Files.writeString(file, """
                {"num":1,"idLocal":"123456","idFiche":"1001"}
                {"num":2,"idLocal":"789012","idFiche":"1002"}
                """);
        Os55ItemReader reader = new Os55ItemReader(file, new ObjectMapper());
        reader.open(new ExecutionContext());

        assertThat(reader.read().getNum()).isEqualTo(1L);
        assertThat(reader.read().getNum()).isEqualTo(2L);
        assertThat(reader.read()).isNull();
        reader.close();
    }

    @Test
    void failsOnMalformedJsonLine() throws Exception {
        Path file = tempDir.resolve("input.ndjson");
        Files.writeString(file, "not-json\n");
        Os55ItemReader reader = new Os55ItemReader(file, new ObjectMapper());
        reader.open(new ExecutionContext());

        assertThatThrownBy(reader::read).isInstanceOf(FlatFileParseException.class);
        reader.close();
    }
}
