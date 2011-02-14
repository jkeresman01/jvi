<?xml version="1.0"?>

<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:myf="http://raelity.com/XSL/Transform"
    exclude-result-prefixes="xs myf">

    <xsl:output method="html"/>

    <xsl:template match="/vimhelp">
        <html>
            <head>
                <title>
                    <xsl:text>Vim: </xsl:text>
                    <xsl:value-of select="@filename"/>
                </title>
                <link rel="stylesheet" href="vimhelp.css" type="text/css"/>
                <style>
                    <xsl:comment>
                        .right { text-align right; color: red }
                    </xsl:comment>
                </style>
            </head>
            <body>
                <xsl:apply-templates select="*"/>
            </body>
        </html>
        <xsl:text>&#xA;</xsl:text>
    </xsl:template>

    <xsl:template match="*">
        <xsl:copy>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>

    <!--
    <xsl:template match="text()">
        <xsl:value-of select="."/>
    </xsl:template>
    -->

    <xsl:template match="table">
        <xsl:choose>
            <xsl:when test="@form = 'ref'">
                <xsl:call-template name="ref-table"/>
            </xsl:when>
            <xsl:when test="@form = 'index'">
                <xsl:call-template name="index-table"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="normal-table"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="normal-table">
        <xsl:call-template name="table-header"/>
        <table border="1">
            <xsl:apply-templates/>
        </table>
    </xsl:template>

    <xsl:template name="ref-table">
        <xsl:call-template name="table-header"/>
        <table border="1">
            <xsl:for-each select="tr">
                <tr valign="top">
                    <xsl:apply-templates select="*[2]"/>
                    <td>
                        <table width="100%">
                            <tr align="right">
                                <xsl:apply-templates select="*[1]"/>
                            </tr>
                            <tr>
                                <xsl:apply-templates select="*[3]"/>
                            </tr>
                        </table>
                    </td>
                </tr>
            </xsl:for-each>
        </table>
    </xsl:template>

    <xsl:template name="index-table">
        <xsl:call-template name="table-header"/>
        <table border="1">
            <xsl:for-each select="tr">
                <tr>
                    <xsl:for-each select="td">
                        <xsl:choose>
                            <xsl:when test="position() = 1">
                                <xsl:apply-templates select=".">
                                    <xsl:with-param name="f_title_linkto"
                                        select="true()" tunnel="yes"/>
                                </xsl:apply-templates>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:apply-templates select="."/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:for-each>
                </tr>
            </xsl:for-each>
        </table>
    </xsl:template>

    <!-- mostly for debug -->
    <xsl:template name="table-header">
        <xsl:if test="@label|@form">
            <h2>
                <xsl:if test="@label">
                    <xsl:value-of select="@label"/>
                </xsl:if>
                <xsl:if test="@form">
                    <xsl:text> (</xsl:text>
                    <xsl:value-of select="@form"/>
                    <xsl:text>)</xsl:text>
                </xsl:if>
            </h2>
        </xsl:if>
    </xsl:template>

    <xsl:template match="link">
        <xsl:param name="f_title_linkto" select="false()" tunnel="yes"/>
        <a href="{@filename}.html#{@linkto}" class="{myf:getClass(@t)}">
            <xsl:if test="$f_title_linkto">
                <xsl:attribute name="title" select="@linkto"/>
            </xsl:if>
            <xsl:apply-templates/>
        </a>
    </xsl:template>

    <xsl:template match="anchor">
        <a name="{normalize-space(string(.))}" class="{myf:getClass(@t)}">
            <xsl:apply-templates/>
        </a>
    </xsl:template>

    <xsl:template match="em">
        <span class="{myf:getClass(@t)}">
            <xsl:apply-templates/>
        </span>
    </xsl:template>

    <!-- map the arg to a class attribute value for html -->
    <xsl:function name="myf:getClass" as="xs:string">
        <xsl:param name="t" as="xs:string"/>
        <!--
        <xsl:value-of select="$t"/>
        -->
        <xsl:choose>
            <xsl:when test="$t = 'hidden'">
                <xsl:value-of select="'d'"/></xsl:when>
            <xsl:when test="$t = 'pipe'">
                <xsl:value-of select="'l'"/></xsl:when>
            <xsl:when test="$t = 'title'">
                <xsl:value-of select="'i'"/></xsl:when>
            <xsl:when test="$t = 'star'">
                <xsl:value-of select="'t'"/></xsl:when>
            <xsl:when test="$t = 'header'">
                <xsl:value-of select="'h'"/></xsl:when>
            <xsl:when test="$t = 'ctrl'">
                <xsl:value-of select="'k'"/></xsl:when>
            <xsl:when test="$t = 'example'">
                <xsl:value-of select="'e'"/></xsl:when>
            <xsl:when test="$t = 'special'">
                <xsl:value-of select="'s'"/></xsl:when>
            <xsl:when test="$t = 'note'">
                <xsl:value-of select="'n'"/></xsl:when>
            <xsl:when test="$t = 'opt'">
                <xsl:value-of select="'o'"/></xsl:when>
            <xsl:when test="$t = 'section'">
                <xsl:value-of select="'c'"/></xsl:when>
            <xsl:when test="$t = 'url'">
                <xsl:value-of select="'u'"/></xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="'XXX'"/></xsl:otherwise>
        </xsl:choose>
    </xsl:function>

</xsl:stylesheet>
