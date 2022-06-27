

package com.bbn.marti.nio.codec;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.nio.util.CodecSource;
import com.google.common.collect.Lists;

public class Codec {

	private ByteCodecFactory serverCodecFactory;
    private ByteCodecFactory clientCodecFactory;
    
    private static final Logger logger = LoggerFactory.getLogger(Codec.class);
	
	public Codec(ByteCodecFactory serverFactory, ByteCodecFactory clientFactory) {
        this.serverCodecFactory = serverFactory;
        this.clientCodecFactory = clientFactory;
	}
	
	private ByteCodecFactory serverFactory() {
        return serverCodecFactory;
	}
    
    private ByteCodecFactory clientFactory() {
        return clientCodecFactory;
    }
    
    public static List<ByteCodecFactory> makeServerFactoryList(List<CodecSource> sources) {
        
        logger.trace("makeServerFactoryList: source: " + sources);
        
        List<ByteCodecFactory> codecsList = new ArrayList<>();
          
        for (CodecSource source : sources) {
            for (Codec codec : source.getCodecs()) {
                codecsList.add(codec.serverFactory());
            }
        }
        
        return codecsList;
    }

    public static List<ByteCodecFactory> makeClientFactoryList(List<CodecSource> sources) {
        List<ByteCodecFactory> codecsList = new ArrayList<>();
        
        for (CodecSource source : sources) {
            for (Codec codec : source.getCodecs()) {
                codecsList.add(codec.clientFactory());
            }
        }
        
        return codecsList;
    }

    public final static CodecSource defaultCodecSource = new CodecSource() {
        @Override
        public ByteCodecFactory serverFactory(Codec codec) {
            return codec.serverFactory();
        }
        
        @Override
        public ByteCodecFactory clientFactory(Codec codec) {
            return codec.clientFactory();
        }

        // no additional codecs in this CodecSource
        @Override
        public List<Codec> getCodecs() {
            return Lists.newArrayList();
        }
        
        @Override
        public String toString() {
            return "DefaultCodecSource - empty codec list";
        }
    };

    @Override
    public String toString() {
        return "Codec [" + serverCodecFactory + ", " + clientCodecFactory + "]";
    }
}
