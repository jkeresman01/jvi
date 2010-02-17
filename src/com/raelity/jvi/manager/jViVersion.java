package com.raelity.jvi.manager;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * version is of the form #.#.# or #.#.#.[x|alpha|beta|rc]#,
 * examples 0.9.1, 0.9.1.beta1
 * also, 0.9.1.beta1.3 for tweaking between exposed releases
 */
public final class jViVersion implements Comparable<jViVersion>
{
    private static Logger LOG = Logger.getLogger(jViVersion.class.getName());

    // in order
    public static final String X = "x";
    public static final String ALPHA = "alpha";
    public static final String BETA = "beta";
    public static final String RC = "rc";
    // following is map in order of suspected quality, these map to values
    // 0, 1, 2, 3
    // a release has none of these tags and compares greater than any of
    // them since it is set to value qualityMap.length, value == 4
    String[] qualityMap = new String[]{X, ALPHA, BETA, RC};
    // Each component of the version is an element of the array.
    // major.minor.micro
    // major.minor.micro.<quality>which
    // If no a|alpha.... then this is the
    private int[] version = new int[6];
    private boolean valid;

    public jViVersion(String s)
    {
        String[] rev = s.split("\\.");
        if (rev.length < 3 || rev.length > 5) {
            init(0, 0, 0, 0, 0);
            return;
        }
        for (int i = 0; i < 3; i++) {
            try {
                version[i] = Integer.parseInt(rev[i]);
            } catch (NumberFormatException ex) {
                LOG.log(Level.SEVERE, null, ex);
                init(0, 0, 0, 0, 0);
                return;
            }
        }
        valid = true;
        if (rev.length == 3)
            // A release version, no quality tag or tweak,
            // it is "better" than those
            version[3] = qualityMap.length;
        else {
            // so this is something between releases
            // version[0:2] has 1.2.3 stored in it
            // rev[3] has string like beta3, rev[4] may have a tweak; beta3.7
            // into rev[0:2] put strings for beta,3,7
            Pattern p = Pattern.compile("(x|alpha|beta|rc)(\\d+)");
            Matcher m = p.matcher(rev[3]);
            // Note, if doesn't match, then version number looks like 1.2.3.x0
            // since version[3:5] left at zero
            if (m.matches()) {
                String q = m.group(1);
                for (int i = 0; i < qualityMap.length; i++) {
                    if (q.equals(qualityMap[i])) {
                        rev[0] = "" + i;
                        break;
                    }
                }
                rev[1] = m.group(2);
                // if there's a tweak on beta1, then copy it, else set to zero
                rev[2] = rev.length == 5 ? rev[4] : "0";
                try {
                    for (int i = 0; i <= 2; i++) {
                        version[i + 3] = Integer.parseInt(rev[i]);
                    }
                } catch (NumberFormatException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            }
        }
        //System.err.println("input: " + s + ", version: " + this);
        //System.err.println("input: " + s + ", version: " + this);
    }

    private void init(int major, int minor, int micro, int qTag, int qVer)
    {
        version[0] = major;
        version[1] = minor;
        version[2] = micro;
        version[3] = qTag;
        version[4] = qVer;
    }

    public boolean isValid()
    {
        return valid;
    }

    public boolean isRelease()
    {
        return version[3] == qualityMap.length;
    }

    public boolean isDevelopment()
    {
        // development releases are x, alpha or any tweaks
        // beta and rc are not (unless tweaked)
        int qTag = version[3];
        return qTag == 0 || qTag == 1 || getTweak() != 0;
    }

    @Override
    public String toString()
    {
        String s = "" + version[0] + "." + version[1] + "." + version[2];
        if (version[3] != qualityMap.length)
            s += "." + qualityMap[version[3]] + version[4];
        if (version[5] != 0)
            s += "." + version[5];
        return s;
    }

    public int getMajor()
    {
        return version[0];
    }

    public int getMinor()
    {
        return version[1];
    }

    public int getMicro()
    {
        return version[2];
    }

    public int getTweak()
    {
        return version[5];
    }

    public String getTag()
    {
        if (isRelease())
            return "";
        return qualityMap[version[3]] + version[4] +
                (getTweak() == 0 ? "" : getTweak());
    }

    public int compareTo(jViVersion v2)
    {
        for (int i = 0; i < version.length; i++) {
            if (version[i] != v2.version[i])
                return version[i] - v2.version[i];
        }
        return 0;
    }
}
