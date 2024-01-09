

package tak.server.cot;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.DecimalFormat;

import com.bbn.marti.remote.util.DateUtil;

import tak.server.util.Association;

/**
 * This class encapsulates a single result of a CoT query from the CoT table.
 *
 */
public class CotElement implements Serializable {

	private static final long serialVersionUID = 765273623441L;
    private static final DecimalFormat haeFormat = new DecimalFormat("#0.0000000000");

    public static final String errorMessage = "Unvalidated due to parse error";
	public String uid = errorMessage;
	public String geom = errorMessage;
	public String cottype = errorMessage;
    public String how = errorMessage;
	public Timestamp servertime = null;
    public Timestamp staletime = null;
	public String prettytime = errorMessage;
	public String detailtext = errorMessage;
	public boolean hasImage = false;
	public String callsign = null;
	public double msl = Double.NaN;
	public double le = Double.NaN;
	public double lat = Double.NaN;
	public double lon = Double.NaN;
	public Association<String,String> urls = null;
	public String iconUrl = errorMessage;
	public String styleUrl = errorMessage;
	public String iconSetPath = errorMessage;
	public Long iconArgbColor = null;
	public String iconSetUid = "";
	public String iconGroup = "";
	public String iconName = "";
	public String hae = errorMessage;
	public Long cotId = null;
	public String groupString = null;

	// ticket #81
	public double course = Double.NaN;
	public double speed = Double.NaN;
	public double ce = Double.NaN;

	/**
	 * Returns the callsign from a Query Result, if present, or the uid, if it is not.
	 */
	public String getCallsign() {
		if (callsign == null || callsign.isEmpty() || callsign.equals(errorMessage))
			return uid;
		return callsign;
	}

	/**
	 * returns a flag indicating whether this query result has been completely initialized
	 */
	public boolean ensure() {
		return true;
	}

	@Override
    public String toString() {
        return "CotElement [uid=" + uid + ", geom=" + geom + ", cottype="
                + cottype + ", servertime=" + servertime + ", prettytime="
                + prettytime + ", detailtext=" + detailtext + ", hasImage="
                + hasImage + ", callsign=" + callsign + ", msl=" + msl
                + ", le=" + le + ", lat=" + lat + ", lon=" + lon + ", urls="
                + urls + ", iconUrl=" + iconUrl + ", styleUrl=" + styleUrl
                + ", iconSetPath=" + iconSetPath + ", iconArgbColor="
                + iconArgbColor + ", iconSetUid=" + iconSetUid + ", iconGroup="
                + iconGroup + ", iconName=" + iconName + ", hae=" + hae + " cotId: " + cotId
                + "course=" + course + ", speed=" + speed + ", ce=" + ce + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((callsign == null) ? 0 : callsign.hashCode());
        result = prime * result + ((cottype == null) ? 0 : cottype.hashCode());
        result = prime * result
                + ((detailtext == null) ? 0 : detailtext.hashCode());
        result = prime * result + ((geom == null) ? 0 : geom.hashCode());
        result = prime * result + ((hae == null) ? 0 : hae.hashCode());
        result = prime * result + (hasImage ? 1231 : 1237);
        result = prime * result
                + ((iconArgbColor == null) ? 0 : iconArgbColor.hashCode());
        result = prime * result
                + ((iconSetPath == null) ? 0 : iconSetPath.hashCode());
        result = prime * result + ((iconUrl == null) ? 0 : iconUrl.hashCode());
        long temp;
        temp = Double.doubleToLongBits(lat);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        
        temp = Double.doubleToLongBits(le);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        
        temp = Double.doubleToLongBits(ce);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        
        temp = Double.doubleToLongBits(lon);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(msl);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result
                + ((prettytime == null) ? 0 : prettytime.hashCode());
        result = prime * result
                + ((servertime == null) ? 0 : servertime.hashCode());
        result = prime * result
                + ((styleUrl == null) ? 0 : styleUrl.hashCode());
        result = prime * result + ((uid == null) ? 0 : uid.hashCode());
        result = prime * result + ((urls == null) ? 0 : urls.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CotElement other = (CotElement) obj;
        if (callsign == null) {
            if (other.callsign != null)
                return false;
        } else if (!callsign.equals(other.callsign))
            return false;
        if (cottype == null) {
            if (other.cottype != null)
                return false;
        } else if (!cottype.equals(other.cottype))
            return false;
        if (detailtext == null) {
            if (other.detailtext != null)
                return false;
        } else if (!detailtext.equals(other.detailtext))
            return false;
        if (geom == null) {
            if (other.geom != null)
                return false;
        } else if (!geom.equals(other.geom))
            return false;
        if (hae == null) {
            if (other.hae != null)
                return false;
        } else if (!hae.equals(other.hae))
            return false;
        if (hasImage != other.hasImage)
            return false;
        if (iconArgbColor == null) {
            if (other.iconArgbColor != null)
                return false;
        } else if (!iconArgbColor.equals(other.iconArgbColor))
            return false;
        if (iconSetPath == null) {
            if (other.iconSetPath != null)
                return false;
        } else if (!iconSetPath.equals(other.iconSetPath))
            return false;
        if (iconUrl == null) {
            if (other.iconUrl != null)
                return false;
        } else if (!iconUrl.equals(other.iconUrl))
            return false;
        if (Double.doubleToLongBits(lat) != Double.doubleToLongBits(other.lat))
            return false;

        if (Double.doubleToLongBits(le) != Double.doubleToLongBits(other.le))
            return false;

        if (Double.doubleToLongBits(ce) != Double.doubleToLongBits(other.ce))
            return false;

        if (Double.doubleToLongBits(lon) != Double.doubleToLongBits(other.lon))
            return false;
        if (Double.doubleToLongBits(msl) != Double.doubleToLongBits(other.msl))
            return false;
        if (prettytime == null) {
            if (other.prettytime != null)
                return false;
        } else if (!prettytime.equals(other.prettytime))
            return false;
        if (servertime == null) {
            if (other.servertime != null)
                return false;
        } else if (!servertime.equals(other.servertime))
            return false;
        if (styleUrl == null) {
            if (other.styleUrl != null)
                return false;
        } else if (!styleUrl.equals(other.styleUrl))
            return false;
        if (uid == null) {
            if (other.uid != null)
                return false;
        } else if (!uid.equals(other.uid))
            return false;
        if (urls == null) {
            if (other.urls != null)
                return false;
        } else if (!urls.equals(other.urls))
            return false;
        return true;
    }

    public String toCotXml() {
        String time = servertime == null ? "" : DateUtil.toCotTime(this.servertime.getTime());
        String staleTime = staletime == null ? "" : DateUtil.toCotTime(this.staletime.getTime());
        String cot =
              "<event version='2.0' "
            + "uid='" + this.uid + "' "
            + "type='" + this.cottype +"' "
            + "time='" + time + "' "
            + "start='" + time + "' "
            + "stale='" + staleTime + "' "
            + "how='" + this.how + "'>"
            + "<point "
            + "lat='" + Double.toString(this.lat) + "' "
            + "lon='" + Double.toString(this.lon) + "' "
            + "hae='" + this.hae + "' "
            + "ce='" + Double.toString(this.ce) + "' "
            + "le='" + Double.toString(this.le) + "' />"
            + this.detailtext
            + "</event>";
        return cot;
    }

    public void setServertime(long servertimeMillis) {
        this.servertime = new Timestamp(servertimeMillis);
    }

    public void setStaletime(long staletimeMillis) {
	    this.staletime = new Timestamp(staletimeMillis);
    }

    public void setHae(double hae) {
        this.hae = haeFormat.format(hae);
    }
}
