package com.thinkaurelius.titan.hadoop.formats.script;

import com.thinkaurelius.titan.hadoop.FaunusVertex;
import com.thinkaurelius.titan.hadoop.config.ModifiableHadoopConfiguration;
import com.thinkaurelius.titan.hadoop.tinkerpop.gremlin.FaunusGremlinScriptEngine;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static com.thinkaurelius.titan.hadoop.formats.script.ScriptConfig.*;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class ScriptRecordWriter extends RecordWriter<NullWritable, FaunusVertex> {
    protected final DataOutputStream out;
    private final ScriptEngine engine = new FaunusGremlinScriptEngine();

    private static final String WRITE_CALL = "write(vertex,output)";
    private static final String VERTEX = "vertex";
    private static final String OUTPUT = "output";

    public ScriptRecordWriter(final DataOutputStream out, final Configuration configuration) throws IOException {
        this.out = out;
        ModifiableHadoopConfiguration faunusConf = ModifiableHadoopConfiguration.of(configuration);
        final FileSystem fs = FileSystem.get(configuration);
        try {
            this.engine.put(OUTPUT, this.out);
            this.engine.eval(new InputStreamReader(fs.open(new Path(faunusConf.getOutputConf(ROOT_NS).get(SCRIPT_FILE)))));
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    public void write(final NullWritable key, final FaunusVertex vertex) throws IOException {
        if (null != vertex) {
            try {
                this.engine.put(VERTEX, vertex);
                this.engine.eval(WRITE_CALL);
            } catch (final ScriptException e) {
                throw new IOException(e.getMessage());
            }
        }
    }

    public synchronized void close(TaskAttemptContext context) throws IOException {
        this.out.close();
    }
}
