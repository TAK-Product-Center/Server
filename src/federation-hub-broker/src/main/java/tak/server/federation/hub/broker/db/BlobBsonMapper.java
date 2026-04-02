package tak.server.federation.hub.broker.db;

import com.atakmap.Tak.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.types.Binary;

public final class BlobBsonMapper {

    public static Document toBson(BinaryBlob blob) {
        Document d = new Document();
        d.append("type", blob.getType().name());
        if (!blob.getData().isEmpty()) {
            d.append("data", new Binary(blob.getData().toByteArray()));
        }
        if (!blob.getFilename().isEmpty())     d.append("filename", blob.getFilename());
        if (blob.getTimestamp() != 0L)         d.append("timestamp", blob.getTimestamp());
        if (!blob.getDescription().isEmpty())  d.append("description", blob.getDescription());

        if (blob.getFederateProvenanceCount() > 0) {
            List<Document> prov = blob.getFederateProvenanceList().stream()
                    .map(p -> new Document("federation_server_id", p.getFederationServerId())
                            .append("federation_server_name", p.getFederationServerName()))
                    .collect(Collectors.toList());
            d.append("federate_provenance", prov);
        }

        if (blob.hasFederateHops()) {
            d.append("federate_hops", new Document("max_hops", blob.getFederateHops().getMaxHops())
                    .append("current_hops", blob.getFederateHops().getCurrentHops()));
        }

        if (blob.hasFederateGroupHopLimits()) {
            List<Document> limits = blob.getFederateGroupHopLimits().getLimitsList().stream()
                    .map(l -> new Document("group_name", l.getGroupName())
                            .append("max_hops", l.getMaxHops())
                            .append("current_hops", l.getCurrentHops()))
                    .collect(Collectors.toList());
            d.append("federate_group_hop_limits", new Document("use_federate_group_hop_limits",
                    blob.getFederateGroupHopLimits().getUseFederateGroupHopLimits())
                    .append("limits", limits));
        }
        return d;
    }

    public static BinaryBlob fromBson(Document d) {
        BinaryBlob.Builder b = BinaryBlob.newBuilder();

        Object typeObj = d.get("type");
        if (typeObj instanceof String) {
            try {
                b.setType(BINARY_TYPES.valueOf((String) typeObj));
            } catch (IllegalArgumentException ignore) { /* fall through as EMPTY */ }
        } else if (typeObj instanceof Number) {
            b.setTypeValue(((Number) typeObj).intValue());
        }

        Object dataObj = d.get("data");
        if (dataObj instanceof Binary) {
            b.setData(com.google.protobuf.ByteString.copyFrom(((Binary) dataObj).getData()));
        } else if (dataObj instanceof byte[]) {
            b.setData(com.google.protobuf.ByteString.copyFrom((byte[]) dataObj));
        }

        String filename = d.getString("filename");
        if (filename != null) b.setFilename(filename);

        Long ts = getLong(d, "timestamp");
        if (ts != null) b.setTimestamp(ts);

        String desc = d.getString("description");
        if (desc != null) b.setDescription(desc);

        @SuppressWarnings("unchecked")
        List<Document> provDocs = (List<Document>) d.get("federate_provenance");
        if (provDocs != null) {
            for (Document p : provDocs) {
                String sid = p.getString("federation_server_id");
                String sname = p.getString("federation_server_name");
                FederateProvenance.Builder pb = FederateProvenance.newBuilder();
                if (sid != null)   pb.setFederationServerId(sid);
                if (sname != null) pb.setFederationServerName(sname);
                b.addFederateProvenance(pb);
            }
        }

        Document hops = d.get("federate_hops", Document.class);
        if (hops != null) {
            Long maxHops = getLong(hops, "max_hops");
            Long curHops = getLong(hops, "current_hops");
            FederateHops.Builder hb = FederateHops.newBuilder();
            if (maxHops != null) hb.setMaxHops(maxHops);
            if (curHops != null) hb.setCurrentHops(curHops);
            b.setFederateHops(hb);
        }

        Document ghl = d.get("federate_group_hop_limits", Document.class);
        if (ghl != null) {
            FederateGroupHopLimits.Builder glb = FederateGroupHopLimits.newBuilder();
            Boolean use = ghl.getBoolean("use_federate_group_hop_limits", Boolean.FALSE);
            glb.setUseFederateGroupHopLimits(use != null && use);
            @SuppressWarnings("unchecked")
            List<Document> limits = (List<Document>) ghl.get("limits");
            if (limits != null) {
                for (Document l : limits) {
                    FederateGroupHopLimit.Builder lb = FederateGroupHopLimit.newBuilder();
                    String gname = l.getString("group_name");
                    if (gname != null) lb.setGroupName(gname);
                    Long lmax = getLong(l, "max_hops");
                    Long lcur = getLong(l, "current_hops");
                    if (lmax != null) lb.setMaxHops(lmax);
                    if (lcur != null) lb.setCurrentHops(lcur);
                    glb.addLimits(lb);
                }
            }
            b.setFederateGroupHopLimits(glb);
        }

        return b.build();
    }

    private static Long getLong(Document d, String key) {
        Object v = d.get(key);
        if (v instanceof Long)    return (Long) v;
        if (v instanceof Integer) return ((Integer) v).longValue();
        if (v instanceof Number)  return ((Number) v).longValue();
        return null;
    }
}
