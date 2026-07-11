package org.schabi.newpipe.extractor.services.bilibili;

import java.util.Locale;
import java.util.SplittableRandom;

import javax.annotation.Nonnull;

public class DeviceForger {

    static public class Device {
        private String userAgent;

        private String webGlVersion;
        private String webGlVersionBase64;

        private String webGLRendererInfo;
        private String webGLRendererInfoBase64;

        private int innerWidth;
        private int innerHeight;

        public String getUserAgent() {
            return userAgent;
        }

        void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }

        public String getWebGlVersion() {
            return webGlVersion;
        }

        public String getWebGlVersionBase64() {
            return webGlVersionBase64;
        }

        void setWebGlVersion(String webGlVersion) {
            this.webGlVersion = webGlVersion;
            this.webGlVersionBase64 = utils.encodeToBase64SubString(webGlVersion);
        }

        public String getWebGLRendererInfo() {
            return webGLRendererInfo;
        }

        public Device(String userAgent, String webGlVersion, String webGLRendererInfo, int innerWidth, int innerHeight) {
            this.userAgent = userAgent;
            this.webGlVersion = webGlVersion;
            this.webGlVersionBase64 = utils.encodeToBase64SubString(webGlVersion);
            this.webGLRendererInfo = webGLRendererInfo;
            this.webGLRendererInfoBase64 = utils.encodeToBase64SubString(webGLRendererInfo);
            this.innerWidth = innerWidth;
            this.innerHeight = innerHeight;
        }

        public String getWebGLRendererInfoBase64() {
            return webGLRendererInfoBase64;
        }

        void setWebGLRendererInfo(String webGLRendererInfo) {
            this.webGLRendererInfo = webGLRendererInfo;
            this.webGLRendererInfoBase64 = utils.encodeToBase64SubString(webGLRendererInfo);
        }

        public int getInnerWidth() {
            return innerWidth;
        }

        void setInnerWidth(int innerWidth) {
            this.innerWidth = innerWidth;
        }

        public int getInnerHeight() {
            return innerHeight;
        }

        void setInnerHeight(int innerHeight) {
            this.innerHeight = innerHeight;
        }

        public String info() {
            return "{"
                    + "UserAgent: " + getUserAgent()
                    + ", WebGlVersion: " + getWebGlVersion()
                    + ", WebGLRendererInfo: " + getWebGLRendererInfo()
                    + "}";
        }
    }

    static class GraphicCard {
        private final String vendor;
        private final String model;

        public GraphicCard(String vendor, String model) {
            this.vendor = vendor;
            this.model = model;
        }

        public String getVendor() {
            return vendor;
        }

        public String getModel() {
            return model;
        }
    }

    final static String UserAgentTemplate = "Mozilla/5.0 (%s) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%d.0.0.0 Safari/537.36";
    final static String DefaultChromiumWebGlVersion = "WebGL 1.0 (OpenGL ES 2.0 Chromium)";
    final static String ChromiumAngleRendererInfoTemplate = "ANGLE (%s, %s Direct3D11 vs_5_0 ps_5_0, D3D11)Google Inc. (%s)";

    static String buildUserAgent(String platform, int version) {
        return String.format(Locale.ROOT, UserAgentTemplate, platform, version);
    }

    static String buildChromiumAngleRendererInfo(String vendor, String gpuWithApi) {
        return String.format(Locale.ROOT, ChromiumAngleRendererInfoTemplate, vendor, gpuWithApi, vendor);
    }

    static GraphicCard randomCard(SplittableRandom random) {
        GraphicCard[] cards = {
                new GraphicCard("AMD", "AMD Radeon 780M Graphics (0x000015BF)"),
                new GraphicCard("AMD", "AMD Radeon RX 5700 (0x0000731F)"),
                new GraphicCard("AMD", "AMD Radeon RX 6500 XT (0x0000743F)"),
                new GraphicCard("AMD", "AMD Radeon RX 6600 (0x000073FF)"),
                new GraphicCard("AMD", "AMD Radeon RX 6750 GRE 10GB (0x000073FF)"),
                new GraphicCard("AMD", "AMD Radeon RX 6750 GRE 12GB (0x000073DF)"),
                new GraphicCard("AMD", "AMD Radeon RX 6750 XT (0x000073DF)"),
                new GraphicCard("AMD", "AMD Radeon RX 6800 XT (0x000073BF)"),
                new GraphicCard("AMD", "AMD Radeon RX 7600S (0x00007480)"),
                new GraphicCard("AMD", "AMD Radeon(TM) Graphics (0x00001636)"),
                new GraphicCard("AMD", "AMD Radeon(TM) Graphics (0x00001638)"),
                new GraphicCard("AMD", "AMD Radeon(TM) Graphics (0x00001681)"),
                new GraphicCard("AMD", "AMD Radeon(TM) Vega 6 Graphics (0x000015DD)"),
                new GraphicCard("Intel", "Intel(R) Arc(TM) A750 Graphics (0x000056A1)"),
                new GraphicCard("Intel", "Intel(R) Arc(TM) Graphics (0x00007D55)"),
                new GraphicCard("Intel", "Intel(R) HD Graphics (0x000022B1)"),
                new GraphicCard("Intel", "Intel(R) HD Graphics 4400 (0x0000041E)"),
                new GraphicCard("Intel", "Intel(R) HD Graphics 510 (0x00001902)"),
                new GraphicCard("Intel", "Intel(R) HD Graphics 520 (0x00001916)"),
                new GraphicCard("Intel", "Intel(R) HD Graphics 530 (0x00001912)"),
                new GraphicCard("Intel", "Intel(R) HD Graphics 530 (0x0000191B)"),
                new GraphicCard("Intel", "Intel(R) HD Graphics 5500 (0x00001616)"),
                new GraphicCard("Intel", "Intel(R) HD Graphics 6000 (0x00001626)"),
                new GraphicCard("Intel", "Intel(R) HD Graphics 610 (0x00005902)"),
                new GraphicCard("Intel", "Intel(R) HD Graphics 620 (0x00005916)"),
                new GraphicCard("Intel", "Intel(R) HD Graphics 630 (0x00005912)"),
                new GraphicCard("Intel", "Intel(R) HD Graphics 630 (0x0000591B)"),
                new GraphicCard("Intel", "Intel(R) Iris(R) Plus Graphics 640 (0x00005926)"),
                new GraphicCard("Intel", "Intel(R) Iris(R) Xe Graphics (0x000046A6)"),
                new GraphicCard("Intel", "Intel(R) Iris(R) Xe Graphics (0x000046A8)"),
                new GraphicCard("Intel", "Intel(R) Iris(R) Xe Graphics (0x0000A7A1)"),
                new GraphicCard("Intel", "Intel(R) UHD Graphics (0x000046A3)"),
                new GraphicCard("Intel", "Intel(R) UHD Graphics (0x00009A60)"),
                new GraphicCard("Intel", "Intel(R) UHD Graphics (0x00009B41)"),
                new GraphicCard("Intel", "Intel(R) UHD Graphics (0x00009BA4)"),
                new GraphicCard("Intel", "Intel(R) UHD Graphics (0x00009BC4)"),
                new GraphicCard("Intel", "Intel(R) UHD Graphics (0x0000A720)"),
                new GraphicCard("Intel", "Intel(R) UHD Graphics (0x0000A721)"),
                new GraphicCard("Intel", "Intel(R) UHD Graphics (0x0000A78B)"),
                new GraphicCard("Intel", "Intel(R) UHD Graphics 610 (0x00009BA8)"),
                new GraphicCard("Intel", "Intel(R) UHD Graphics 620 (0x00003EA0)"),
                new GraphicCard("Intel", "Intel(R) UHD Graphics 620 (0x00005917)"),
                new GraphicCard("Intel", "Intel(R) UHD Graphics 630 (0x00003E91)"),
                new GraphicCard("Intel", "Intel(R) UHD Graphics 630 (0x00003E92)"),
                new GraphicCard("Intel", "Intel(R) UHD Graphics 630 (0x00003E98)"),
                new GraphicCard("Intel", "Intel(R) UHD Graphics 630 (0x00009BC5)"),
                new GraphicCard("Intel", "Intel(R) UHD Graphics 730 (0x00004682)"),
                new GraphicCard("Intel", "Intel(R) UHD Graphics 730 (0x00004692)"),
                new GraphicCard("Intel", "Intel(R) UHD Graphics 750 (0x00004C8A)"),
                new GraphicCard("Intel", "Intel(R) UHD Graphics 770 (0x00004680)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce GT 710 (0x0000128B)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce GT 730 (0x00000F02)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce GT 730 (0x00001287)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce GT 1010 (0x00001D02)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce GTX 650 (0x00000FC6)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce GTX 750 (0x00001381)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce GTX 750 Ti (0x00001380)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce GTX 960 (0x00001401)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce GTX 1050 (0x00001C81)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce GTX 1050 Ti (0x00001C82)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce GTX 1060 6GB (0x00001C03)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce GTX 1070 (0x00001B81)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce GTX 1650 Ti (0x00001F95)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce GTX 1660 (0x00002184)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce GTX 1660 SUPER (0x000021C4)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce GTX 965M (0x00001427)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce RTX 2060 (0x00001F08)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce RTX 2070 SUPER (0x00001E84)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce RTX 3050 (0x00002584)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce RTX 3060 (0x00002504)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce RTX 3060 (0x00002544)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce RTX 3060 Laptop GPU (0x00002520)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce RTX 3060 Laptop GPU (0x00002560)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce RTX 3060 Ti (0x00002414)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce RTX 3060 Ti (0x00002489)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce RTX 3060 Ti (0x000024C9)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce RTX 3070 (0x00002484)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce RTX 3070 (0x00002488)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce RTX 3080 (0x00002206)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce RTX 3080 Ti (0x00002208)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce RTX 4050 Laptop GPU (0x000028E1)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce RTX 4060 Laptop GPU (0x000028A0)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce RTX 4060 Laptop GPU (0x000028E0)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce RTX 4060 Ti (0x00002803)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce RTX 4070 (0x00002786)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce RTX 4070 Laptop GPU (0x00002820)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce RTX 4070 SUPER (0x00002783)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce RTX 4080 (0x00002704)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce RTX 4080 SUPER (0x00002702)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce RTX 4090 (0x00002684)"),
                new GraphicCard("NVIDIA", "NVIDIA GeForce RTX 4090 D (0x00002685)"),
                new GraphicCard("NVIDIA", "NVIDIA Quadro P2000 (0x00001C30)"),
        };
        return cards[random.nextInt(cards.length)];
    }

    static Device forgeDevice(SplittableRandom random) {
        // currently we only forge device with
        // * Modern Chrome Browser (so User Agent are frozen)
        // * Windows 10/11 X64 Operate System
        int chromiumVersion = (random.nextInt(8)) + 130;
        GraphicCard graphicCard = randomCard(random);
        Device device = new Device(
                buildUserAgent("Windows NT 10.0; Win64; x64", chromiumVersion),
                DefaultChromiumWebGlVersion,
                buildChromiumAngleRendererInfo(graphicCard.getVendor(), graphicCard.getModel()),
                1920 - 60 - random.nextInt(60),
                1080 - 90 - random.nextInt(60)
        );
        return device;
    }

    static private Device currentDevice = null;

    @Nonnull
    static public Device requireRandomDevice() {
        if (currentDevice == null) {
            regenerateRandomDevice();
        }
        return currentDevice;
    }

    static public void regenerateRandomDevice() {
        currentDevice = forgeDevice(new SplittableRandom());
    }

}
