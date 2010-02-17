package com.raelity.jvi.manager;

import com.raelity.jvi.ViOutputStream;
import com.raelity.jvi.core.G;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
 * The message, followed by a line starting with &lt;EOT&gt;
 * <p/>
 * Note:    there can be any number of motd-link and motd-message and
 * they are output in the order encounter
 *
 */
class Motd
{
    private static Logger LOG = Logger.getLogger(Motd.class.getName());

    private jViVersion latestRelease;
    private jViVersion latestBeta;
    private String motdVersion;
    private String downloadTarget;
    private int messageNumber;
    private String message;
    private boolean valid;
    private boolean outputNetworkInfo;
    private boolean outputBasicInfo;
    // following could be a list of pairs of link:text
    List<OutputHandler> outputList =
            new ArrayList<OutputHandler>(5);

    static void get(ActionListener action)
    {
        new GetMotd(action).start();
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
        //String lines[] = motd.split("\n");
        Pattern p;
        Matcher m;
        p = Pattern.compile("^motd-version:\\s*(\\S+)", Pattern.MULTILINE);
        m = p.matcher(s);
        if (m.find())
            motdVersion = m.group(1);
        p = Pattern.compile("^jVi-release:\\s*(\\S+)", Pattern.MULTILINE);
        m = p.matcher(s);
        if (m.find())
            latestRelease = new jViVersion(m.group(1));
        p = Pattern.compile("^jVi-beta:\\s*(\\S+)", Pattern.MULTILINE);
        m = p.matcher(s);
        if (m.find())
            latestBeta = new jViVersion(m.group(1));
        p = Pattern.compile("^jVi-download-target:\\s*(\\S+)", Pattern.MULTILINE);
        m = p.matcher(s);
        if (m.find())
            downloadTarget = m.group(1);
        p =
                Pattern.compile("^(?:motd-link:\\s*(\\S+)" + "|motd-message:)" +
                "\\s*\\n" + "(.*?)\\s*\\n<EOT>",
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

    void outputOnce()
    {
        if (outputNetworkInfo)
            return;
        if (outputBasicInfo && !valid)
            return;
        output(G.isHideVersion.getBoolean() ? ViOutputStream.PRI_LOW
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
            else
                // In this else, should be able to assert that !isRelease()
                if (ViManager.version.isDevelopment())
                    tagCurrent = " (development release)";
        vios.println("Running: " + ViManager.getReleaseString() + tagCurrent);
        if (hasNewer != null)
            vios.printlnLink(downloadTarget, hasNewer);
        if (latestBeta != null && latestBeta.isValid())
            if (latestBeta.compareTo(ViManager.version) > 0)
                vios.printlnLink(downloadTarget,
                                 "Beta or release candidate available: " +
                        latestBeta);
        for (int i = 0; i < outputList.size(); i++) {
            OutputHandler outputHandler = outputList.get(i);
            outputHandler.output(vios);
        }
        //if(message != null)
        //    vios.println(message);
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
        private ActionListener action;

        GetMotd(ActionListener action) {
            this.action = action;
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

                action.actionPerformed(new ActionEvent(
                        new Motd(sb.toString()), 0, null));
            } catch (IOException ex) {
                if(ViManager.isDebugAtHome())
                    ex.printStackTrace();
            }
        }
    }
}
