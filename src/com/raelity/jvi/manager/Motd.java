package com.raelity.jvi.manager;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.raelity.jvi.ViOutputStream;
import com.raelity.jvi.core.G;

/**
 * Parse and output the jVi motd data.
 * The fields are:
 * <br/>    motd-version: version-number
 * <br/>    jVi-release: release-number
 * <br/>    jVi-beta: release-number
 * <br/>    jVi-download-target: where to download new jvi stuff
 * <br/>    motd-link: link-with-no-spaces
 * <br/>    display text for link
 * <br/>    &lt;EOT&gt;
 * <br/>    motd-message:
 * <br/>    The message, followed by a line starting with &lt;EOT&gt;
 * <br/>
 * <br/> Added later, typically they are both specified to point to the latest.
 *       The corresponding file name in the "new release" message link here.
 * <br/>    jVi-release-link:
 * <br/>    jVi-beta-link:
 * <p/>
 * Note:    there can be any number of motd-link and motd-message and
 * they are output in the order encounter
 *
 */
class Motd
{
    private static final Logger LOG = Logger.getLogger(Motd.class.getName());

    private jViVersion latestRelease;
    private jViVersion latestBeta;
    private String motdVersion;
    private String downloadTarget;
    private boolean valid;
    private boolean outputNetworkInfo;
    private boolean outputBasicInfo;
    private String linkRelease;
    private String linkBeta;

    // following could be a list of pairs of link:text
    List<OutputHandler> outputList =
            new ArrayList<OutputHandler>(5);

    static void get(ChangeListener change)
    {
        new GetMotd(change).start();
    }

    Motd()
    {
    }

    boolean getValid()
    {
        return valid;
    }

    Motd(String s)
    {
        String match;
        motdVersion = find("^motd-version:\\s*(\\S+)", Pattern.MULTILINE, s);
        if(ViManager.isDebugAtHome())
            LOG.log(Level.INFO, "Motd version:{0}", motdVersion);
        match = find("^jVi-release:\\s*(\\S+)", Pattern.MULTILINE, s);
        if (match != null)
            latestRelease = new jViVersion(match);
        match = find("^jVi-beta:\\s*(\\S+)", Pattern.MULTILINE, s);
        if (match != null)
            latestBeta = new jViVersion(match);
        // NOT USED downloadTarget
        downloadTarget = find("^jVi-download-target:\\s*(\\S+)",
                              Pattern.MULTILINE, s);
        linkRelease = find("^jVi-release-link:\\s*(\\S+)",
                              Pattern.MULTILINE, s);
        linkBeta = find("^jVi-beta-link:\\s*(\\S+)",
                              Pattern.MULTILINE, s);

        Pattern p;
        Matcher m;
        p = Pattern.compile("^(?:motd-link:\\s*(\\S+)"
                          + "|motd-message:)"
                          + "\\s*\\n"
                          + "(.*?)\\s*\\n<EOT>",
                          Pattern.MULTILINE | Pattern.DOTALL);
        m = p.matcher(s);
        while (m.find()) {
            if (m.start(1) >= 0)
                outputList.add(new OutputLink(m.group(1), m.group(2)));
            else
                outputList.add(new OutputString(m.group(2)));
        }
        valid = true;
    }

    private String find(String regex, int flags, String input)
    {
        Pattern p;
        Matcher m;
        p = Pattern.compile(regex, flags);
        m = p.matcher(input);
        if (m.find())
            return m.group(1);
        return null;
    }

    void outputOnce()
    {
        if (outputNetworkInfo)
            return;
        if (outputBasicInfo && !valid)
            return;
        output(G.isHideVersion() ? ViOutputStream.PRI_LOW
                : ViOutputStream.PRI_NORMAL);
    }

    void output()
    {
        output(ViOutputStream.PRI_NORMAL);
    }

    void output(int priority)
    {
        if (!valid) {
            ViOutputStream vios =
                    ViManager.createOutputStream(null, ViOutputStream.OUTPUT,
                                                 ViManager.getReleaseString(),
                                                 priority);
            vios.close();
            outputBasicInfo = true;
            return;
        }
        outputNetworkInfo = true;
        ViOutputStream vios =
                ViManager.createOutputStream(null, ViOutputStream.OUTPUT,
                                             "jVi Version Information", priority);
        String tagCurrent = "";
        String hasNewer = null;
        if (latestRelease != null && latestRelease.isValid())
            if (latestRelease.compareTo(ViManager.version) > 0)
                hasNewer = "Newer release available: " + latestRelease;
            else if (latestRelease.compareTo(ViManager.version) == 0)
                tagCurrent = " (This is the latest release)";
            else {
                // In this else, should be able to assert that !isRelease()
                if (ViManager.version.isDevelopment())
                    tagCurrent = " (development release)";
            }
        vios.println("Running: " + ViManager.getReleaseString() + tagCurrent);
        if (hasNewer != null) {
            if(linkRelease != null)
                vios.printlnLink(linkRelease, hasNewer);
            else
                vios.println(hasNewer);
        }
        if (latestBeta != null && latestBeta.isValid())
            if (latestBeta.compareTo(ViManager.version) > 0) {
                String betaMsg = "Beta or release candidate available: "
                                      + latestBeta;
                if(linkBeta != null)
                    vios.printlnLink(linkBeta, betaMsg);
                else
                    vios.println(betaMsg);
            }
        for (int i = 0; i < outputList.size(); i++) {
            OutputHandler outputHandler = outputList.get(i);
            outputHandler.output(vios);
        }
        vios.close();
    }

    private interface OutputHandler
    {

        void output(ViOutputStream vios);
    }

    private class OutputString implements OutputHandler
    {

        String msg;

        OutputString(String msg)
        {
            this.msg = msg;
        }

        @Override
        public void output(ViOutputStream vios)
        {
            vios.println(msg);
        }

        @Override
        public String toString()
        {
            return msg;
        }
    }

    private class OutputLink implements OutputHandler
    {

        String link;
        String text;

        public OutputLink(String link, String text)
        {
            this.link = link;
            this.text = text;
        }

        @Override
        public void output(ViOutputStream vios)
        {
            vios.printlnLink(link, text);
        }

        @Override
        public String toString()
        {
            return link + " : " + text;
        }
    }

    private static class GetMotd extends Thread
    {
        private static final int BUF_LEN = 1024;
        private static final int MAX_MSG = 8 * 1024;
        private ChangeListener change;

        GetMotd(ChangeListener change) {
            this.change = change;
        }

        @Override
        public void run()
        {
            URL url = null;
            try {
                String s = System.getProperty("com.raelity.jvi.motd");
                if(s != null)
                    System.err.println("DEBUG MOTD: " + s);
                if(s == null)
                    s = "http://jvi.sourceforge.net/motd";
                URI uri = new URI(s);
                url = uri.toURL();
            } catch (MalformedURLException ex) {
                LOG.log(Level.SEVERE, null, ex);
            } catch (URISyntaxException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
            if(url == null)
                return;

            // Read the remote file into a string
            // We *know* that the decoder will never have unprocessed
            // bytes in it, US-ASCII ==> 1 byte per char.
            // So use a simple algorithm.
            try {
                URLConnection c = url.openConnection();
                InputStream in = c.getInputStream();
                byte b[] = new byte[BUF_LEN];
                ByteBuffer bb = ByteBuffer.wrap(b);
                StringBuilder sb = new StringBuilder();
                Charset cset = Charset.forName("US-ASCII");
                int n;
                int total = 0;
                while((n = in.read(b)) > 0 && total < MAX_MSG) {
                    bb.position(0);
                    bb.limit(n);
                    CharBuffer cb = cset.decode(bb);
                    sb.append(cb.toString());
                    total += n;
                }
                in.close();

                change.stateChanged(new ChangeEvent(new Motd(sb.toString())));
            } catch (IOException ex) {
                if(ViManager.isDebugAtHome())
                    LOG.log(Level.SEVERE, null, ex);
            }
        }
    }
}
