package org.springframework.security.oauth2.provider.endpoint;

import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.HtmlUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Controller for displaying the approval page for the authorization server.
 *
 * <p>
 * @deprecated See the <a href="https://github.com/spring-projects/spring-security/wiki/OAuth-2.0-Migration-Guide">OAuth 2.0 Migration Guide</a> for Spring Security 5.
 *
 * @author Dave Syer
 */
@FrameworkEndpoint
@SessionAttributes("authorizationRequest")
@Deprecated
public class WhitelabelApprovalEndpoint {

    @RequestMapping("/oauth/confirm_access")
    public ModelAndView getAccessConfirmation(Map<String, Object> model, HttpServletRequest request) throws Exception {
        final String approvalContent = createTemplate(model, request);
        if (request.getAttribute("_csrf") != null) {
            model.put("_csrf", request.getAttribute("_csrf"));
        }
        View approvalView = new View() {
            @Override
            public String getContentType() {
                return "text/html";
            }

            @Override
            public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
                response.setContentType(getContentType());
                response.getWriter().append(approvalContent);
            }
        };
        return new ModelAndView(approvalView, model);
    }

    protected String createTemplate(Map<String, Object> model, HttpServletRequest request) {
        AuthorizationRequest authorizationRequest = (AuthorizationRequest) model.get("authorizationRequest");
        String clientId = authorizationRequest.getClientId();

        StringBuilder builder = new StringBuilder();

        builder.append("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n");
        builder.append("    <link rel=\"icon\" type=\"image/x-icon\" href=\"/Marti/favicon.ico\" />\n");
        builder.append("    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n");
        builder.append("    <link rel=\"stylesheet\" href=\"/Marti/css/4.4.1/bootstrap.min.css\">\n");
        builder.append("    <link type=\"text/css\" href=\"/Marti/jquery/jquery-ui.css\" rel=\"stylesheet\" media=\"all\" />\n");
        builder.append("    <link rel=\"stylesheet\" type=\"text/css\" href=\"/Marti/jquery/jquery.jnotify.css\" />\n");
        builder.append("    <script type=\"text/javascript\" src=\"/Marti/jquery/jquery-3.5.0.js\"></script>\n");
        builder.append("    <script type=\"text/javascript\" src=\"/Marti/jquery/jquery-ui.min.js\"></script>\n");
        builder.append("    <script type=\"text/javascript\" src=\"/Marti/jquery/jquery.jnotify.min.js\"></script>\n");
        builder.append("   <meta name=\"viewport\" content=\"width=device-width,initial-scale=1,maximum-scale=1,user-scalable=0,viewport-fit=cover\">\n");
        builder.append("\n");
        builder.append("    <title>OAuth2 Authorization</title>\n");
        builder.append("</head>\n");
        builder.append("\n");
        builder.append("<body>\n");
        builder.append("\n");
        builder.append("<div id=\"header\">\n");
        builder.append("    <style>\n");
        builder.append("        .tak_logo {\n");
        builder.append("            width:75px;\n");
        builder.append("            height:75px;\n");
        builder.append("            display:inline-block;\n");
        builder.append("            margin-top: -4px;\n");
        builder.append("            margin-left: 0px;\n");
        builder.append("            filter: drop-shadow(0 2px 3px #cecece);\n");
        builder.append("        }\n");
        builder.append("\n");
        builder.append("        body {\n");
        builder.append("            margin: 0;\n");
        builder.append("            padding: 0;\n");
        builder.append("            background-color: #444444;\n");
        builder.append("            height: 100vh;\n");
        builder.append("        }\n");
        builder.append("\n");
        builder.append("        #login .container #login-row #login-column #login-box {\n");
        builder.append("            margin-top: 40px;\n");
        builder.append("            max-width: 600px;\n");
        builder.append("            height: 400px;\n");
        builder.append("            border: 1px solid #9C9C9C;\n");
        builder.append("            background-color: #fdfdfd;\n");
        builder.append("        }\n");
        builder.append("        #login .container #login-row #login-column #login-box #login-form {\n");
        builder.append("            padding: 30px;\n");
        builder.append("        }\n");
        builder.append("    </style>\n");
        builder.append("</div>\n");
        builder.append("\n");
        builder.append("<div class=\"alert alert-warning\" style=\"display:none;\" id=\"error-alert\" align=\"center\">\n");
        builder.append("    <b>You have entered an invalid username or password</b>\n");
        builder.append("</div>\n");
        builder.append("\n");
        builder.append("<div id=\"login\">\n");
        builder.append("    <div class=\"container\">\n");
        builder.append("        <div id=\"login-row\" class=\"row justify-content-center align-items-center\">\n");
        builder.append("            <div id=\"login-column\" class=\"col-md-6\">\n");
        builder.append("                <div id=\"login-box\" class=\"col-md-12\">\n");
        builder.append("                    <div id=\"auth-div\" style=\"padding: 30px\">\n");
        builder.append("                        <div class=\"form-group\" align=\"center\">\n");
        builder.append("                            <div class=\"menu-button\">\n");
        builder.append("                                <div class=\"tak_logo\">\n");
        builder.append("                                    <svg xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:cc=\"http://creativecommons.org/ns#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:svg=\"http://www.w3.org/2000/svg\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:sodipodi=\"http://sodipodi.sourceforge.net/DTD/sodipodi-0.dtd\" xmlns:inkscape=\"http://www.inkscape.org/namespaces/inkscape\" id=\"svg2\" version=\"1.1\" inkscape:version=\"0.91 r13725\" viewBox=\"0 0 842.5 805\" sodipodi:docname=\"TAK MIL Vector.svg\" inkscape:export-filename=\"simple logo.png\" inkscape:export-xdpi=\"14.635015\" inkscape:export-ydpi=\"14.635015\">\n");
        builder.append("                                        <metadata id=\"metadata8\">\n");
        builder.append("                                            <rdf:rdf>\n");
        builder.append("                                                <cc:work rdf:about=\"\">\n");
        builder.append("                                                    <dc:format>image/svg+xml</dc:format>\n");
        builder.append("                                                    <dc:type rdf:resource=\"http://purl.org/dc/dcmitype/StillImage\">\n");
        builder.append("                                                        <dc:title></dc:title>\n");
        builder.append("                                                    </dc:type>\n");
        builder.append("                                                </cc:work>\n");
        builder.append("                                            </rdf:rdf>\n");
        builder.append("                                        </metadata>\n");
        builder.append("                                        <defs id=\"defs6\">\n");
        builder.append("                                            <lineargradient inkscape:collect=\"always\" id=\"linearGradient4893\">\n");
        builder.append("                                                <stop style=\"stop-color:#6e6e6e;stop-opacity:1\" offset=\"0\" id=\"stop4895\"></stop>\n");
        builder.append("                                                <stop id=\"stop4905\" offset=\"0.5\" style=\"stop-color:#ffffff;stop-opacity:1\"></stop>\n");
        builder.append("                                                <stop style=\"stop-color:#6e6e6e;stop-opacity:1\" offset=\"1\" id=\"stop4897\"></stop>\n");
        builder.append("                                            </lineargradient>\n");
        builder.append("                                            <lineargradient inkscape:collect=\"always\" xlink:href=\"#linearGradient4893\" id=\"linearGradient4903\" x1=\"143.88919\" y1=\"402.5\" x2=\"698.61081\" y2=\"402.5\" gradientUnits=\"userSpaceOnUse\"> </lineargradient>\n");
        builder.append("                                        </defs>\n");
        builder.append("                                        <sodipodi:namedview pagecolor=\"#ffffff\" bordercolor=\"#666666\" borderopacity=\"1\" objecttolerance=\"10\" gridtolerance=\"10\" guidetolerance=\"10\" inkscape:pageopacity=\"0\" inkscape:pageshadow=\"2\" inkscape:window-width=\"1200\" inkscape:window-height=\"1857\" id=\"namedview4\" showgrid=\"false\" inkscape:object-nodes=\"true\" inkscape:snap-intersection-paths=\"false\" inkscape:snap-smooth-nodes=\"true\" inkscape:zoom=\"0.82920348\" inkscape:cx=\"407.2691\" inkscape:cy=\"-13.264391\" inkscape:window-x=\"-8\" inkscape:window-y=\"172\" inkscape:window-maximized=\"1\" inkscape:current-layer=\"svg2\"></sodipodi:namedview>\n");
        builder.append("                                        <path style=\"fill:url(#linearGradient4903);fill-opacity:1.0\" d=\"m 406.60249,743.76344 c -6.7326,-5.0886 -17.8661,-13.9223 -24.7411,-19.6304 -17.5932,-14.6071 -61.3248,-58.6747 -77.3979,-77.9925 -89.1195,-107.1108 -141.2829,-228.3029 -156.296,-363.125 -2.4118,-21.659 -4.7208,-60.7744 -4.2056,-71.2469 l 0.3995,-8.12186 16.25,-4.09782 c 88.0141,-22.19478 180.4684,-72.7928 244.7366,-133.93844 8.1052,-7.71133 15.2043,-13.84047 15.7759,-13.62031 0.5717,0.22016 8.8464,7.55763 18.3884,16.3055 42.0414,38.54243 81.2242,65.35266 132.3491,90.55791 35.4975,17.50066 74.22794,31.81699 110.62404,40.89109 l 15.626,3.89579 0.4097,7.5 c 0.6261,11.46447 -2.1385,58.72484 -4.7948,81.96374 -15.4285,134.9784 -63.9842,251.2889 -146.33724,350.5363 -15.7729,19.0086 -58.4623,60.9748 -77.4027,76.0914 -20.6481,16.4796 -45.1907,33.2836 -48.6113,33.2836 -1.3924,0 -8.0401,-4.1635 -14.7726,-9.2521 z\" id=\"path4751-5\" inkscape:connector-curvature=\"0\"></path>\n");
        builder.append("                                        <path style=\"fill:#0b0b0b\" d=\"M 404.2674,773.79865 C 361.0723,744.12024 313.994,699.12276 273.363,648.68009 l -12.8375,-15.9375 -98.7002,0 c -84.593,0 -98.7003,-0.2546 -98.7003,-1.78131 0,-2.16073 60.6753,-212.6732 62.446,-216.65619 1.1455,-2.5765 2.0856,-2.8125 11.2035,-2.8125 5.4741,0 10.29,-0.54526 10.7019,-1.21169 0.4118,-0.66642 -1.2275,-8.68205 -3.643,-17.8125 -14.4819,-54.74121 -21.5824,-106.80396 -22.8304,-167.39816 -0.8416,-40.86041 -1.7687,-37.51684 10.872,-39.2115 29.3974,-3.94113 84.9181,-22.90458 125.7763,-42.95968 57.4562,-28.20219 100.0499,-58.509169 145.733,-103.694201 l 17.1342,-16.94745 22.8658,22.43419 c 43.0863,42.273132 80.7773,69.193761 134.3453,95.955561 43.9907,21.97713 98.4353,40.62374 132.2704,45.30099 11.1927,1.54724 10.625,0.17761 10.625,25.63673 0,56.845 -7.2877,121.35496 -19.6182,173.65771 -6.4845,27.50539 -6.9064,25.54953 5.6059,25.98886 9.3224,0.32734 10.4813,0.64108 11.5436,3.125 1.7489,4.08921 61.2187,214.4932 61.2187,216.59158 0,1.54356 -13.3312,1.79723 -95.3125,1.8137 -52.4218,0.01 -95.9277,0.4324 -96.6797,0.9375 -0.752,0.5051 -7.0102,7.94961 -13.9072,16.54336 -30.4811,37.97996 -66.0563,72.81804 -102.2894,100.16989 -22.3778,16.8927 -47.0729,32.33011 -51.7182,32.33011 -1.2008,0 -8.0411,-4.02478 -15.2006,-8.94394 z m 29.2357,-26.56186 c 14.8308,-9.77796 30.9867,-22.10731 47.9152,-36.56651 25.8514,-22.08057 74.3229,-73.45597 72.3364,-76.67014 -1.2468,-2.01745 -259.3819,-1.71396 -259.3721,0.30495 0.028,5.7043 63.3051,71.64062 89.1687,92.9155 22.2036,18.26427 33.7758,26.65976 36.8268,26.71751 1.583,0.03 7.4892,-2.98562 13.125,-6.70131 z m 235.5566,-337.3067 c 1.4275,-1.79756 10.2179,-37.7889 13.9673,-57.1875 7.091,-36.68705 10.7672,-70.10838 12.5807,-114.375 0.8575,-20.93036 0.7701,-27.82834 -0.3655,-28.87439 C 694.4215,208.73729 685.3125,205.99648 675,203.40251 588.0145,181.52255 502.829,134.57486 435.2796,71.286961 l -14.7205,-13.79177 -10.6807,9.8112 c -26.7374,24.56071 -47.9424,41.637539 -70.925,57.117539 -53.2187,35.84548 -117.4005,65.00368 -175.2034,79.59596 -8.9375,2.25626 -17.0047,4.79171 -17.9272,5.63434 -1.3873,1.26723 -1.4381,6.4968 -0.294,30.255 2.6398,54.82109 9.7496,100.90961 23.3838,151.58336 2.4972,9.28125 5.0979,17.57812 5.7794,18.4375 0.9838,1.24051 51.9221,1.5625 247.183,1.5625 195.2601,0 246.1996,-0.322 247.1847,-1.5625 z\" id=\"path4690-7\" inkscape:connector-curvature=\"0\" sodipodi:nodetypes=\"sscsscscssssscsssssscscsssssssccscsssscsscssscsscss\"></path>\n");
        builder.append("                                        <path style=\"fill:#080808\" d=\"m 379.99995,381.41191 c -2.6442,-0.72999 -3.1863,-1.71607 -3.5233,-6.40976 -0.5776,-8.04522 0.7591,-13.49898 3.6935,-15.06943 6.4551,-3.45463 12.3298,3.39385 12.3298,14.37345 0,5.05378 -0.4026,5.93261 -3.2064,6.99859 -3.5564,1.35215 -4.7346,1.36573 -9.2936,0.10712 z m 21.8256,-0.99104 c -4.4383,-4.43838 0.3505,-17.89705 6.3682,-17.89705 0.9167,0 3.1598,1.28419 4.9845,2.85375 3.9748,3.41903 5.9356,14.10714 2.9363,16.00636 -3.0201,1.91249 -12.02,1.30592 -14.289,-0.96306 z m 25.1774,0.4773 c -1.9597,-1.43392 -2.2528,-2.79284 -1.6864,-7.81815 0.6929,-6.14732 4.0357,-10.5562 8.0037,-10.5562 5.192,0 11.2402,12.68611 8.2916,17.39155 -1.6468,2.62793 -11.4584,3.288 -14.6089,0.9828 z m 27.997,0.0537 c -2.8128,-1.49278 -3.1243,-2.42141 -3.1178,-9.2949 0.01,-5.74016 0.5966,-8.28785 2.3812,-10.25976 3.2011,-3.53715 7.5183,-3.30579 10.564,0.56612 2.8113,3.57396 4.9322,16.30094 3.033,18.20013 -2.0654,2.0654 -9.5855,2.52642 -12.8604,0.78841 z m -39.2483,-19.7636 c -5.9457,-4.47073 -7.6371,-4.53705 -13.5331,-0.53066 l -4.6117,3.13375 -4.1159,-3.57544 c -8.6965,-7.55438 -11.765,-7.57613 -20.2305,-0.14334 -5.3836,4.72685 -7.6377,4.24334 -10.3435,-2.21863 -1.838,-4.38982 -1.8635,-9.74646 -0.1115,-23.45507 0.2969,-2.32275 -0.6443,-2.55967 -13.276,-3.34165 -7.4775,-0.4629 -15.0075,-1.19604 -16.7333,-1.62919 -4.4093,-1.10666 -11.3594,-6.66716 -13.4948,-10.79665 -2.5505,-4.93212 -2.252,-8.64115 1.3114,-16.29588 2.5692,-5.51887 3.1038,-8.5422 3.0609,-17.31163 -0.029,-5.84375 -0.4731,-14.41156 -0.9878,-19.0396 -0.555,-4.98971 -0.4424,-9.31426 0.2766,-10.625 0.6668,-1.21571 5.3614,-7.11764 10.4323,-13.11537 11.1398,-13.17603 12.5312,-15.69583 11.8568,-21.47273 -0.6008,-5.14613 -2.1867,-4.91477 -3.3743,0.49227 -0.8817,4.0145 -16.2675,19.17081 -18.6557,18.37738 -1.7016,-0.56536 -4.6434,-25.01657 -3.7137,-30.86701 1.7411,-10.956 16.4918,-34.01782 28.6938,-44.86081 7.4451,-6.61591 20.1406,-12.87615 33.6734,-16.6045 8.9952,-2.47824 12.9515,-2.75506 39.375,-2.75506 26.4234,0 30.3797,0.27682 39.375,2.75506 13.5327,3.72835 26.2282,9.98859 33.6733,16.6045 12.202,10.84299 26.9527,33.90481 28.6938,44.86081 0.9297,5.85044 -2.0121,30.30165 -3.7137,30.86701 -2.3881,0.79343 -17.774,-14.36288 -18.6557,-18.37738 -1.1876,-5.40704 -2.7735,-5.6384 -3.3743,-0.49227 -0.6744,5.7769 0.717,8.2967 11.8569,21.47268 5.0709,5.99773 9.7654,11.89966 10.4322,13.11537 0.719,1.31074 0.8316,5.63529 0.2767,10.625 -0.5147,4.62804 -0.9593,13.19585 -0.9878,19.0396 -0.043,8.76943 0.4916,11.79276 3.0608,17.31163 3.5634,7.65473 3.8619,11.36376 1.3114,16.29588 -2.1354,4.12949 -9.0855,9.68999 -13.4948,10.79665 -1.7258,0.43315 -9.2558,1.16629 -16.7333,1.62919 -12.6316,0.78198 -13.5729,1.0189 -13.276,3.34165 1.752,13.70861 1.7265,19.06525 -0.1115,23.45507 -2.7058,6.46197 -4.9599,6.94548 -10.3435,2.21863 -8.4655,-7.43278 -11.534,-7.41103 -20.2304,0.14334 l -4.116,3.57544 -4.6117,-3.13375 c -5.9094,-4.01553 -7.5393,-3.94291 -13.833,0.61626 -2.8472,2.0625 -5.3222,3.71148 -5.5001,3.6644 -0.1778,-0.0471 -2.5163,-1.73459 -5.1966,-3.75 z m -0.022,-29.60189 c 0.4275,-1.89063 1.5442,-6.52218 2.4815,-10.29233 0.994,-3.99851 1.2412,-7.31779 0.5931,-7.96591 -1.6055,-1.60543 -6.7996,4.03945 -10.5782,11.4963 -4.5085,8.89703 -4.285,10.19944 1.7501,10.19944 4.4068,0 5.0651,-0.39333 5.7535,-3.4375 z m 21.7703,1.52685 c 0,-2.93437 -6.8963,-15.91604 -9.8929,-18.62231 -4.0808,-3.68558 -5.3029,-1.17995 -3.3183,6.80313 0.9373,3.77015 2.054,8.4017 2.4815,10.29233 0.6884,3.04417 1.3467,3.4375 5.7535,3.4375 3.6312,0 4.9762,-0.51641 4.9762,-1.91065 z m -47.3939,-34.39354 c 2.7536,-1.40477 7.2461,-4.57741 9.9833,-7.0503 5.4387,-4.91357 7.4278,-4.69314 5.3204,0.58965 -0.7438,1.86438 -1.808,4.51479 -2.3651,5.88979 -1.9233,4.7475 3.0231,-0.53185 8.6989,-9.28446 6.8604,-10.57937 8.7917,-12.23344 11.5958,-9.93109 1.1492,0.94355 4.4869,5.41254 7.417,9.93109 5.6758,8.75261 10.6222,14.03196 8.6989,9.28446 -0.5571,-1.375 -1.6214,-4.02541 -2.3651,-5.88979 -2.1074,-5.28279 -0.1184,-5.50322 5.3204,-0.58965 2.7372,2.47289 7.3656,5.71488 10.2854,7.20443 5.0117,2.55676 5.9064,2.62492 15.9938,1.21851 23.1915,-3.23342 34.424,-13.39408 30.0411,-27.17453 -1.9972,-6.27938 -8.5739,-16.64397 -10.5613,-16.64397 -0.8633,0 -9.1016,3.61006 -18.3072,8.02236 -18.3028,8.77266 -29.6794,12.60091 -33.6767,11.33224 -3.3925,-1.07676 -5.07,-5.78195 -5.3403,-14.9796 -0.2557,-8.70019 -2.0369,-9.40873 -4.2804,-1.70265 -0.8579,2.94705 -2.4048,6.20317 -3.4375,7.23582 -1.67,1.67005 -2.085,1.67005 -3.755,0 -1.0327,-1.03265 -2.5796,-4.28877 -3.4375,-7.23582 -2.2435,-7.70608 -4.0247,-6.99754 -4.2804,1.70265 -0.2703,9.19765 -1.9478,13.90284 -5.3403,14.9796 -3.9973,1.26867 -15.3739,-2.55958 -33.6767,-11.33224 -9.2056,-4.4123 -17.4439,-8.02236 -18.3072,-8.02236 -1.9874,0 -8.5641,10.36459 -10.5613,16.64397 -4.0611,12.7686 5.7213,22.92867 25.5432,26.5295 12.4341,2.25877 15.1246,2.16462 20.7938,-0.72761 z\" id=\"path4768\" inkscape:connector-curvature=\"0\"></path>\n");
        builder.append("                                        <path style=\"fill:#000000\" d=\"\" id=\"path4855\" inkscape:connector-curvature=\"0\"></path>\n");
        builder.append("                                        <path style=\"fill:#ffffff;fill-opacity:1\" d=\"m 279.11424,580.10645 c -1.65294,-2.35992 -1.94587,-8.25437 -1.94587,-39.1571 l 0,-36.37897 2.89306,-2.7179 c 2.49989,-2.34853 4.33074,-2.71789 13.47186,-2.71789 17.33193,0 16.13508,-3.09704 16.13508,41.75215 0,45.35925 1.32783,41.99785 -16.59005,41.99785 -11.00224,0 -12.18269,-0.23485 -13.96408,-2.77814 z m 62.87485,-0.24107 c -1.5353,-3.36973 -1.3696,-3.72355 25.5227,-54.48079 10.9686,-20.70251 14.7893,-26.875 16.6352,-26.875 3.0301,0 15.4024,23.08883 15.4736,28.87649 0.03,2.42452 -4.6229,13.54423 -12.4222,29.6875 l -12.4699,25.81101 -15.6819,0 c -15.3677,0 -15.7094,-0.0605 -17.0575,-3.01921 z m 94.7513,0.20671 c -0.9546,-1.54688 -3.087,-5.34375 -4.7388,-8.4375 l -3.0032,-5.625 -17.79,-0.34683 c -17.4621,-0.34042 -17.7892,-0.39802 -17.7445,-3.125 0.025,-1.52799 2.697,-7.84067 5.9375,-14.02817 5.0221,-9.58909 6.3995,-11.3084 9.3295,-11.64553 2.1372,-0.24591 3.4375,-1.14469 3.4375,-2.37603 0,-1.08928 -5.0625,-11.80776 -11.25,-23.81885 -6.1875,-12.0111 -11.2421,-22.75439 -11.2325,-23.87398 0.01,-1.11958 3.1466,-8.11593 6.9711,-15.54745 5.716,-11.10708 7.4035,-13.443 9.4813,-13.125 1.9647,0.30068 9.3477,13.82804 33.1485,60.7362 17.0529,33.60894 30.3082,61.16383 29.9154,62.1875 -0.5665,1.47631 -3.6601,1.83814 -15.7158,1.83814 -14.2187,0 -15.1019,-0.14833 -16.746,-2.8125 z m 52.7685,0.74654 c -1.3867,-1.66994 -1.7825,-12.93603 -2.0648,-58.77009 l -0.3492,-56.70412 3.2269,-3.22686 c 3.0724,-3.07239 3.7675,-3.20606 14.5229,-2.79241 16.2444,0.62474 16.0737,0.32744 16.0737,27.99915 0,11.91451 0.5042,21.97434 1.1204,22.35518 0.6162,0.38083 3.2618,-2.1156 5.8791,-5.54765 5.5742,-7.30937 8.628,-8.72385 11.746,-5.4406 1.2109,1.2751 14.208,18.91211 28.8824,39.19336 20.4254,28.2295 26.6886,37.7542 26.714,40.625 l 0.033,3.75 -16.4798,0.3482 -16.4798,0.34821 -6.751,-8.47321 c -3.713,-4.66026 -12.4943,-15.92633 -19.514,-25.0357 -7.0196,-9.10938 -13.2654,-16.5625 -13.8794,-16.5625 -0.614,0 -1.2918,10.45265 -1.5062,23.2281 -0.4717,28.10872 0.3947,26.7719 -17.3507,26.7719 -9.7348,0 -12.4443,-0.40494 -13.8236,-2.06596 z m 61.6414,-81.0374 c -4.2526,-5.83185 -7.7319,-11.65371 -7.7319,-12.93749 0,-1.28377 4.194,-7.687 9.32,-14.22939 10.5503,-13.46556 10.5799,-13.47976 28.1425,-13.47976 9.1548,0 11.2472,0.38362 13.0684,2.39601 2.713,2.99785 3.0556,2.3259 -14.5533,28.54149 -17.0187,25.33672 -16.8839,25.2904 -28.2457,9.70914 z m -301.20378,-8.84252 c -2.5634,-1.79547 -2.77814,-2.93561 -2.77814,-14.75 0,-8.2921 0.52859,-13.33271 1.5,-14.30412 1.10894,-1.10894 12.79027,-1.5 44.80626,-1.5 51.48266,0 47.44376,-1.36516 47.44376,16.03592 0,17.69858 3.4939,16.46408 -46.59688,16.46408 -35.62534,0 -41.99569,-0.27935 -44.375,-1.94588 z\" id=\"path4872\" inkscape:connector-curvature=\"0\"></path>\n");
        builder.append("                                    </svg>\n");
        builder.append("                                </div>\n");
        builder.append("                            </div>\n");
        builder.append("                        </div>\n");

        builder.append("						<div class=\"form-group\">\n");
        builder.append("							<label for=\"username\" class=\"text-info\">Authorization Approval</label><br>\n");
        builder.append("							<p>Do you authorize \"").append(HtmlUtils.htmlEscape(clientId)).append("\" to access your TAK Server account?</p>\n");
        builder.append("						</div>\n");

        String requestPath = ServletUriComponentsBuilder.fromContextPath(request).build().getPath();
        if (requestPath == null) {
            requestPath = "";
        }

        builder.append("						<div class=\"form-group\" align=\"center\">\n");
        builder.append("							<form id=\"confirmationForm\" name=\"confirmationForm\" action=\"").append(requestPath).append("/oauth/authorize\" method=\"post\">\n");
        builder.append("								<input name=\"user_oauth_approval\" value=\"true\" type=\"hidden\">\n");
        builder.append("								<input name=\"authorize\" value=\"Authorize\" type=\"submit\" class=\"btn btn-dark\">\n");
        builder.append("							</form>				\n");
        builder.append("						</div>\n");
        builder.append("						<div class=\"form-group\" align=\"center\">\n");
        builder.append("							<form id=\"denialForm\" name=\"denialForm\" action=\"").append(requestPath).append("/oauth/authorize\" method=\"post\">\n");
        builder.append("								<input name=\"user_oauth_approval\" value=\"false\" type=\"hidden\">\n");
        builder.append("								<input name=\"deny\" value=\"Deny\" type=\"submit\" class=\"btn btn-dark\">\n");
        builder.append("							</form>\n");
        builder.append("						</div>\n");

        String csrfTemplate = null;
        CsrfToken csrfToken = (CsrfToken) (model.containsKey("_csrf") ? model.get("_csrf") : request.getAttribute("_csrf"));
        if (csrfToken != null) {
            csrfTemplate = "<input type=\"hidden\" name=\"" + HtmlUtils.htmlEscape(csrfToken.getParameterName()) +
                    "\" value=\"" + HtmlUtils.htmlEscape(csrfToken.getToken()) + "\" />";
        }
        if (csrfTemplate != null) {
            builder.append(csrfTemplate);
        }

//        String authorizeInputTemplate = "<label><input name=\"authorize\" value=\"Authorize\" type=\"submit\"/></label></form>";
//
//        if (model.containsKey("scopes") || request.getAttribute("scopes") != null) {
//            builder.append(createScopes(model, request));
//            builder.append(authorizeInputTemplate);
//        } else {
//            builder.append(authorizeInputTemplate);
//            builder.append("<form id=\"denialForm\" name=\"denialForm\" action=\"");
//            builder.append(requestPath).append("/oauth/authorize\" method=\"post\">");
//            builder.append("<input name=\"user_oauth_approval\" value=\"false\" type=\"hidden\"/>");
//            if (csrfTemplate != null) {
//                builder.append(csrfTemplate);
//            }
//            builder.append("<label><input name=\"deny\" value=\"Deny\" type=\"submit\"/></label></form>");
//        }

        builder.append("					</div>\n");
        builder.append("                </div>\n");
        builder.append("            </div>\n");
        builder.append("        </div>\n");
        builder.append("    </div>\n");
        builder.append("</div>\n");
        builder.append("\n");
        builder.append("\n");
        builder.append("</body></html>\n");

        return builder.toString();
    }

    private CharSequence createScopes(Map<String, Object> model, HttpServletRequest request) {
        StringBuilder builder = new StringBuilder("<ul>");
        @SuppressWarnings("unchecked")
        Map<String, String> scopes = (Map<String, String>) (model.containsKey("scopes") ?
                model.get("scopes") : request.getAttribute("scopes"));
        for (String scope : scopes.keySet()) {
            String approved = "true".equals(scopes.get(scope)) ? " checked" : "";
            String denied = !"true".equals(scopes.get(scope)) ? " checked" : "";
            scope = HtmlUtils.htmlEscape(scope);

            builder.append("<li><div class=\"form-group\">");
            builder.append(scope).append(": <input type=\"radio\" name=\"");
            builder.append(scope).append("\" value=\"true\"").append(approved).append(">Approve</input> ");
            builder.append("<input type=\"radio\" name=\"").append(scope).append("\" value=\"false\"");
            builder.append(denied).append(">Deny</input></div></li>");
        }
        builder.append("</ul>");
        return builder.toString();
    }
}