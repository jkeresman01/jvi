package com.raelity.jvi.manager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.raelity.jvi.ViOutputStream;
import com.raelity.jvi.ViOutputStream.COLOR;
import com.raelity.jvi.ViOutputStream.FLAGS;
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
    List<OutputHandler> outputList = new ArrayList<>(5);

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
        EnumSet<FLAGS> flags = EnumSet.noneOf(FLAGS.class);
        flags.add(G.isHideVersion() ? FLAGS.RAISE_NO : FLAGS.RAISE_YES);
        output(flags);
    }

    void output()
    {
        output(EnumSet.of(FLAGS.RAISE_YES));
    }

    void output(EnumSet<FLAGS> flags)
    {
        if (!valid) {
            ViOutputStream vios =
                    ViManager.createOutputStream(null, ViOutputStream.MAIN,
                                                 ViManager.getReleaseString(),
                                                 flags);
            vios.close();
            outputBasicInfo = true;
            return;
        }
        outputNetworkInfo = true;
        try (ViOutputStream vios = ViManager.createOutputStream(null, ViOutputStream.MAIN,
                null, flags)) {
            boolean needNL = true;
            vios.print("jVi Info, ");
            vios.print("Running: " + ViManager.getReleaseString());
            if (latestRelease != null && latestRelease.isValid())
                if (latestRelease.compareTo(ViManager.version) > 0) {
                    vios.print("; ");
                    vios.print("newer release available ", COLOR.WARNING);
                    if(linkRelease != null) {
                        vios.printlnLink(latestRelease.toString(), linkRelease);
                        needNL = false;
                    }
                } else if (latestRelease.compareTo(ViManager.version) == 0) {
                    vios.print("; ");
                    vios.println("this is the latest release", COLOR.SUCCESS);
                    needNL = false;
                } else {
                    if (ViManager.version.isDevelopment()) {
                        vios.print("; ");
                        vios.println("development release", COLOR.DEBUG);
                        needNL = false;
                    }
                }
            if(needNL)
                vios.println("");
            if (latestBeta != null && latestBeta.isValid())
                if (latestBeta.compareTo(ViManager.version) > 0) {
                    vios.print("Beta or release candidate available: ", COLOR.DEBUG);
                    if(linkBeta != null)
                        vios.printlnLink(latestBeta.toString(), linkBeta);
                    else
                        vios.println(latestBeta.toString());
                }
            for (int i = 0; i < outputList.size(); i++) {
                OutputHandler outputHandler = outputList.get(i);
                outputHandler.output(vios);
            }
        }
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
            vios.printlnLink(text, link);
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
        private final ChangeListener change;

        GetMotd(ChangeListener change) {
            this.change = change;
        }

        @Override
        public void run()
        {
            URL url = null;
            try {
                // URI uri = new URI("file:///C:/other/mydir/myfile.txt");
                String s = System.getProperty("com.raelity.jvi.motd");
                if(s != null) {
                    System.err.println("DEBUG MOTD: " + s);
                    File file = new File(new URI(s));
                    System.err.println("DEBUG MOTD: " + file);
                    if(!file.canRead())
                        s = null;
                }
                if(s == null)
                    s = "http://jvi.sourceforge.net/motd";
                URI uri = new URI(s);
                url = uri.toURL();
            } catch (MalformedURLException | URISyntaxException ex) {
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
                StringBuilder sb;
                try (InputStream in = c.getInputStream()) {
                    byte b[] = new byte[BUF_LEN];
                    // workaround jdk 9+ compiler issue, call it a Buffer
                    // https://github.com/apache/felix/pull/114
                    Buffer bb = ByteBuffer.wrap(b);
                    sb = new StringBuilder();
                    Charset cset = Charset.forName("US-ASCII");
                    int n;
                    int total = 0;
                    while((n = in.read(b)) > 0 && total < MAX_MSG) {
                        bb.position(0);
                        bb.limit(n);
                        CharBuffer cb = cset.decode((ByteBuffer)bb);
                        sb.append(cb.toString());
                        total += n;
                    }
                }

                change.stateChanged(new ChangeEvent(new Motd(sb.toString())));
            } catch (IOException ex) {
                if(ViManager.isDebugAtHome())
                    LOG.log(Level.SEVERE, null, ex);
            }
        }
    }
}
