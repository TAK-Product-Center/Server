package com.bbn.marti.test.shared.data.generated;

import com.bbn.marti.config.AuthType;
import com.bbn.marti.test.shared.data.GroupSetProfiles;
import com.bbn.marti.test.shared.data.connections.*;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfiles;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import com.bbn.marti.test.shared.data.users.UserFilter;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created on 10/22/15.
 */
public class ImmutableConnections extends AbstractConnection {
    private static final HashMap<String, ImmutableConnections> valueMap = new HashMap<>();

    //////////////////////////
    // Begin Generated Users
    ////////////////////////// 68EBC0C6-82C3-4C2F-9779-9191B5B432EC
    public static final ImmutableConnections sCLI_tcp = new ImmutableConnections("sCLI_tcp", ImmutableServerProfiles.SERVER_CLI, BaseConnections.tcp, 17862, "sCLI_tcp_anonuser_t_A", "sCLI_tcp_anonuser_t_B", "NV_sCLI_tcp_anonuser_t_B", "NV_sCLI_tcp_anonuser_t", "NV_sCLI_tcp_anonuser_t_A", "sCLI_tcp_anonuser_t");
    public static final ImmutableConnections s0_saproxy = new ImmutableConnections("s0_saproxy", ImmutableServerProfiles.SERVER_0, BaseConnections.saproxy, 17735, "s0_saproxy_anonuser_t_A", "NV_s0_saproxy_anonuser_t_B", "NV_s0_saproxy_anonuser_t_A", "s0_saproxy_anonuser_t", "s0_saproxy_anonuser_t_B", "NV_s0_saproxy_anonuser_t");
    public static final ImmutableConnections sCLI_substcp = new ImmutableConnections("sCLI_substcp", ImmutableServerProfiles.SERVER_CLI, BaseConnections.substcp, 17892, "sCLI_substcp_anonuser_t", "sCLI_substcp_anonuser_t_A", "sCLI_substcp_anonuser_t_B", "NV_sCLI_substcp_anonuser_t", "NV_sCLI_substcp_anonuser_t_A", "NV_sCLI_substcp_anonuser_t_B");
    public static final ImmutableConnections s1_submcast = new ImmutableConnections("s1_submcast", ImmutableServerProfiles.SERVER_1, BaseConnections.submcast, 17794, "s1_submcast_anonuser_t_A", "s1_submcast_anonuser_t_B", "NV_s1_submcast_anonuser_t_A", "s1_submcast_anonuser_t", "NV_s1_submcast_anonuser_t_B", "NV_s1_submcast_anonuser_t");
    public static final ImmutableConnections s0_mcast = new ImmutableConnections("s0_mcast", ImmutableServerProfiles.SERVER_0, BaseConnections.mcast, 17734, "s0_mcast_anonuser_t_A", "NV_s0_mcast_anonuser_t", "s0_mcast_anonuser_t_B", "NV_s0_mcast_anonuser_t_B", "NV_s0_mcast_anonuser_t_A", "s0_mcast_anonuser_t");
    public static final ImmutableConnections sCLI_tcp2f = new ImmutableConnections("sCLI_tcp2f", ImmutableServerProfiles.SERVER_CLI, BaseConnections.tcp2f, 17883, "sCLI_tcp2f_anonuser_2f", "NV_sCLI_tcp2f_anonuser_2f_A", "NV_sCLI_tcp2f_anonuser_2f", "NV_sCLI_tcp2f_anonuser_2f_B", "sCLI_tcp2f_anonuser_2f_A", "sCLI_tcp2f_anonuser_2f_B");
    public static final ImmutableConnections sCLI_saproxyA = new ImmutableConnections("sCLI_saproxyA", ImmutableServerProfiles.SERVER_CLI, BaseConnections.saproxy, 17868, "NV_sCLI_saproxyA_anonuser_t_B", "NV_sCLI_saproxyA_anonuser_t", "NV_sCLI_saproxyA_anonuser_t_A", "sCLI_saproxyA_anonuser_t", "sCLI_saproxyA_anonuser_t_B", "sCLI_saproxyA_anonuser_t_A");
    public static final ImmutableConnections sCLI_udp3f = new ImmutableConnections("sCLI_udp3f", ImmutableServerProfiles.SERVER_CLI, BaseConnections.udp3f, 17886, "NV_sCLI_udp3f_anonuser_3f_A", "sCLI_udp3f_anonuser_3f_B", "sCLI_udp3f_anonuser_3f_A", "sCLI_udp3f_anonuser_3f", "NV_sCLI_udp3f_anonuser_3f", "NV_sCLI_udp3f_anonuser_3f_B");
    public static final ImmutableConnections sCLI_mcast3f = new ImmutableConnections("sCLI_mcast3f", ImmutableServerProfiles.SERVER_CLI, BaseConnections.mcast3f, 17889, "NV_sCLI_mcast3f_anonuser_3f", "NV_sCLI_mcast3f_anonuser_3f_A", "NV_sCLI_mcast3f_anonuser_3f_B", "sCLI_mcast3f_anonuser_3f", "sCLI_mcast3f_anonuser_3f_A", "sCLI_mcast3f_anonuser_3f_B");
    public static final ImmutableConnections sCLI_tls = new ImmutableConnections("sCLI_tls", ImmutableServerProfiles.SERVER_CLI, BaseConnections.tls, 17870, "NV_sCLI_tls_anonuser_t_B", "sCLI_tls_anonuser_t", "NV_sCLI_tls_anonuser_t_A", "NV_sCLI_tls_anonuser_t", "sCLI_tls_anonuser_t_A", "sCLI_tls_anonuser_t_B");
    public static final ImmutableConnections s1_mcast01 = new ImmutableConnections("s1_mcast01", ImmutableServerProfiles.SERVER_1, BaseConnections.mcast01, 17788, "NV_s1_mcast01_anonuser_01f_B", "NV_s1_mcast01_anonuser_01f_A", "NV_s1_mcast01_anonuser_01f", "s1_mcast01_anonuser_01f", "s1_mcast01_anonuser_01f_A", "s1_mcast01_anonuser_01f_B");
    public static final ImmutableConnections s2_authssl = new ImmutableConnections("s2_authssl", ImmutableServerProfiles.SERVER_2, BaseConnections.authssl, 17805, "s2_authssl_authuser_f", "NV_s2_authssl_authuser2_2f", "s2_authssl_authuser12_012f", "NV_s2_authssl_authuser_f", "s2_authssl_authuser0_0f", "NV_s2_authssl_authuser0_0f", "NV_s2_authssl_authuser012_012f", "s2_authssl_authuser01_01f", "NV_s2_authssl_authuser3_3f", "NV_s2_authssl_authuser01_01f", "s2_authssl_authuser2_2f", "NV_s2_authssl_authuser12_012f", "s2_authssl_authuser3_3f", "s2_authssl_authuser012_012f");
    public static final ImmutableConnections s0_stcpA = new ImmutableConnections("s0_stcpA", ImmutableServerProfiles.SERVER_0, BaseConnections.stcp, 17733, "s0_stcpA_anonuser_t", "s0_stcpA_anonuser_t_A", "s0_stcpA_anonuser_t_B", "NV_s0_stcpA_anonuser_t", "NV_s0_stcpA_anonuser_t_B", "NV_s0_stcpA_anonuser_t_A");
    public static final ImmutableConnections s2_udp3f = new ImmutableConnections("s2_udp3f", ImmutableServerProfiles.SERVER_2, BaseConnections.udp3f, 17820, "NV_s2_udp3f_anonuser_3f", "NV_s2_udp3f_anonuser_3f_A", "s2_udp3f_anonuser_3f", "NV_s2_udp3f_anonuser_3f_B", "s2_udp3f_anonuser_3f_A", "s2_udp3f_anonuser_3f_B");
    public static final ImmutableConnections s1_authtls = new ImmutableConnections("s1_authtls", ImmutableServerProfiles.SERVER_1, BaseConnections.authtls, 17774, "NV_s1_authtls_authuser12_012f", "s1_authtls_authuser01_01f", "NV_s1_authtls_authuser012_012f", "NV_s1_authtls_authuser01_01f", "s1_authtls_authuser012_012f", "NV_s1_authtls_authuser_f", "s1_authtls_authuser3_3f", "s1_authtls_authuser_f", "s1_authtls_authuser12_012f", "NV_s1_authtls_authuser3_3f", "s1_authtls_authuser2_2f", "NV_s1_authtls_authuser2_2f", "s1_authtls_authuser0_0f", "NV_s1_authtls_authuser0_0f");
    public static final ImmutableConnections sCLI_ssl = new ImmutableConnections("sCLI_ssl", ImmutableServerProfiles.SERVER_CLI, BaseConnections.ssl, 17869, "NV_sCLI_ssl_anonuser_t", "NV_sCLI_ssl_anonuser_t_B", "NV_sCLI_ssl_anonuser_t_A", "sCLI_ssl_anonuser_t_A", "sCLI_ssl_anonuser_t_B", "sCLI_ssl_anonuser_t");
    public static final ImmutableConnections s0_authstcpA = new ImmutableConnections("s0_authstcpA", ImmutableServerProfiles.SERVER_0, BaseConnections.authstcp, 17746, "NV_s0_authstcpA_authuser012_012f", "NV_s0_authstcpA_authuser01_01f", "s0_authstcpA_authuser01_01f", "NV_s0_authstcpA_authuser0_0f", "NV_s0_authstcpA_authuser_f", "NV_s0_authstcpA_authuser12_012f", "NV_s0_authstcpA_authuser2_2f", "s0_authstcpA_authuser_f", "s0_authstcpA_authuser12_012f", "s0_authstcpA_authuser012_012f", "NV_s0_authstcpA_authuser3_3f", "s0_authstcpA_authuser0_0f", "s0_authstcpA_authuser3_3f", "s0_authstcpA_authuser2_2f");
    public static final ImmutableConnections s2_substcp = new ImmutableConnections("s2_substcp", ImmutableServerProfiles.SERVER_2, BaseConnections.substcp, 17826, "NV_s2_substcp_anonuser_t_B", "NV_s2_substcp_anonuser_t_A", "s2_substcp_anonuser_t_B", "s2_substcp_anonuser_t", "s2_substcp_anonuser_t_A", "NV_s2_substcp_anonuser_t");
    public static final ImmutableConnections s0_stcp0 = new ImmutableConnections("s0_stcp0", ImmutableServerProfiles.SERVER_0, BaseConnections.stcp0, 17744, "s0_stcp0_anonuser_0f", "NV_s0_stcp0_anonuser_0f_B", "NV_s0_stcp0_anonuser_0f_A", "s0_stcp0_anonuser_0f_B", "s0_stcp0_anonuser_0f_A", "NV_s0_stcp0_anonuser_0f");
    public static final ImmutableConnections s2_tcp2f = new ImmutableConnections("s2_tcp2f", ImmutableServerProfiles.SERVER_2, BaseConnections.tcp2f, 17817, "NV_s2_tcp2f_anonuser_2f", "NV_s2_tcp2f_anonuser_2f_B", "NV_s2_tcp2f_anonuser_2f_A", "s2_tcp2f_anonuser_2f", "s2_tcp2f_anonuser_2f_A", "s2_tcp2f_anonuser_2f_B");
    public static final ImmutableConnections s0_stcp3 = new ImmutableConnections("s0_stcp3", ImmutableServerProfiles.SERVER_0, BaseConnections.stcp3, 17762, "NV_s0_stcp3_anonuser_3f", "s0_stcp3_anonuser_3f", "NV_s0_stcp3_anonuser_3f_A", "s0_stcp3_anonuser_3f_B", "NV_s0_stcp3_anonuser_3f_B", "s0_stcp3_anonuser_3f_A");
    public static final ImmutableConnections s1_udp = new ImmutableConnections("s1_udp", ImmutableServerProfiles.SERVER_1, BaseConnections.udp, 17764, "s1_udp_anonuser_t_B", "s1_udp_anonuser_t_A", "s1_udp_anonuser_t", "NV_s1_udp_anonuser_t_A", "NV_s1_udp_anonuser_t_B", "NV_s1_udp_anonuser_t");
    public static final ImmutableConnections sCLI_stcp01t = new ImmutableConnections("sCLI_stcp01t", ImmutableServerProfiles.SERVER_CLI, BaseConnections.stcp01t, 17879, "NV_sCLI_stcp01t_anonuser_01t", "sCLI_stcp01t_anonuser_01t", "sCLI_stcp01t_anonuser_01t_B", "sCLI_stcp01t_anonuser_01t_A", "NV_sCLI_stcp01t_anonuser_01t_A", "NV_sCLI_stcp01t_anonuser_01t_B");
    public static final ImmutableConnections s0_subtcp = new ImmutableConnections("s0_subtcp", ImmutableServerProfiles.SERVER_0, BaseConnections.subtcp, 17758, "s0_subtcp_anonuser_t", "NV_s0_subtcp_anonuser_t_B", "NV_s0_subtcp_anonuser_t_A", "s0_subtcp_anonuser_t_A", "s0_subtcp_anonuser_t_B", "NV_s0_subtcp_anonuser_t");
    public static final ImmutableConnections s0_stcp01t = new ImmutableConnections("s0_stcp01t", ImmutableServerProfiles.SERVER_0, BaseConnections.stcp01t, 17747, "NV_s0_stcp01t_anonuser_01t_A", "NV_s0_stcp01t_anonuser_01t_B", "NV_s0_stcp01t_anonuser_01t", "s0_stcp01t_anonuser_01t", "s0_stcp01t_anonuser_01t_A", "s0_stcp01t_anonuser_01t_B");
    public static final ImmutableConnections s0_stcp01 = new ImmutableConnections("s0_stcp01", ImmutableServerProfiles.SERVER_0, BaseConnections.stcp01, 17742, "s0_stcp01_anonuser_01f_A", "s0_stcp01_anonuser_01f_B", "s0_stcp01_anonuser_01f", "NV_s0_stcp01_anonuser_01f", "NV_s0_stcp01_anonuser_01f_B", "NV_s0_stcp01_anonuser_01f_A");
    public static final ImmutableConnections s1_saproxy = new ImmutableConnections("s1_saproxy", ImmutableServerProfiles.SERVER_1, BaseConnections.saproxy, 17768, "NV_s1_saproxy_anonuser_t_A", "s1_saproxy_anonuser_t", "NV_s1_saproxy_anonuser_t", "s1_saproxy_anonuser_t_B", "NV_s1_saproxy_anonuser_t_B", "s1_saproxy_anonuser_t_A");
    public static final ImmutableConnections s1_tls = new ImmutableConnections("s1_tls", ImmutableServerProfiles.SERVER_1, BaseConnections.tls, 17771, "s1_tls_anonuser_t_A", "NV_s1_tls_anonuser_t", "s1_tls_anonuser_t_B", "NV_s1_tls_anonuser_t_B", "NV_s1_tls_anonuser_t_A", "s1_tls_anonuser_t");
    public static final ImmutableConnections s1_stcp01 = new ImmutableConnections("s1_stcp01", ImmutableServerProfiles.SERVER_1, BaseConnections.stcp01, 17775, "NV_s1_stcp01_anonuser_01f", "s1_stcp01_anonuser_01f_B", "s1_stcp01_anonuser_01f_A", "s1_stcp01_anonuser_01f", "NV_s1_stcp01_anonuser_01f_A", "NV_s1_stcp01_anonuser_01f_B");
    public static final ImmutableConnections s1_udp3f = new ImmutableConnections("s1_udp3f", ImmutableServerProfiles.SERVER_1, BaseConnections.udp3f, 17787, "s1_udp3f_anonuser_3f", "NV_s1_udp3f_anonuser_3f_B", "NV_s1_udp3f_anonuser_3f_A", "s1_udp3f_anonuser_3f_B", "s1_udp3f_anonuser_3f_A", "NV_s1_udp3f_anonuser_3f");
    public static final ImmutableConnections s1_mcast = new ImmutableConnections("s1_mcast", ImmutableServerProfiles.SERVER_1, BaseConnections.mcast, 17767, "s1_mcast_anonuser_t_A", "NV_s1_mcast_anonuser_t", "s1_mcast_anonuser_t_B", "NV_s1_mcast_anonuser_t_B", "NV_s1_mcast_anonuser_t_A", "s1_mcast_anonuser_t");
    public static final ImmutableConnections s0_authstcp = new ImmutableConnections("s0_authstcp", ImmutableServerProfiles.SERVER_0, BaseConnections.authstcp, 17745, "NV_s0_authstcp_authuser01_01f", "NV_s0_authstcp_authuser12_012f", "NV_s0_authstcp_authuser_f", "s0_authstcp_authuser_f", "s0_authstcp_authuser012_012f", "s0_authstcp_authuser01_01f", "NV_s0_authstcp_authuser0_0f", "NV_s0_authstcp_authuser2_2f", "NV_s0_authstcp_authuser3_3f", "NV_s0_authstcp_authuser012_012f", "s0_authstcp_authuser12_012f", "s0_authstcp_authuser0_0f", "s0_authstcp_authuser3_3f", "s0_authstcp_authuser2_2f");
    public static final ImmutableConnections s2_authstcpA = new ImmutableConnections("s2_authstcpA", ImmutableServerProfiles.SERVER_2, BaseConnections.authstcp, 17812, "NV_s2_authstcpA_authuser_f", "NV_s2_authstcpA_authuser3_3f", "NV_s2_authstcpA_authuser012_012f", "s2_authstcpA_authuser01_01f", "s2_authstcpA_authuser2_2f", "s2_authstcpA_authuser12_012f", "s2_authstcpA_authuser0_0f", "NV_s2_authstcpA_authuser12_012f", "s2_authstcpA_authuser012_012f", "NV_s2_authstcpA_authuser2_2f", "NV_s2_authstcpA_authuser01_01f", "NV_s2_authstcpA_authuser0_0f", "s2_authstcpA_authuser_f", "s2_authstcpA_authuser3_3f");
    public static final ImmutableConnections s1_saproxyA = new ImmutableConnections("s1_saproxyA", ImmutableServerProfiles.SERVER_1, BaseConnections.saproxy, 17769, "NV_s1_saproxyA_anonuser_t_A", "s1_saproxyA_anonuser_t_B", "NV_s1_saproxyA_anonuser_t_B", "s1_saproxyA_anonuser_t_A", "NV_s1_saproxyA_anonuser_t", "s1_saproxyA_anonuser_t");
    public static final ImmutableConnections s2_submcast = new ImmutableConnections("s2_submcast", ImmutableServerProfiles.SERVER_2, BaseConnections.submcast, 17827, "s2_submcast_anonuser_t_B", "s2_submcast_anonuser_t", "s2_submcast_anonuser_t_A", "NV_s2_submcast_anonuser_t", "NV_s2_submcast_anonuser_t_B", "NV_s2_submcast_anonuser_t_A");
    public static final ImmutableConnections s2_authtls = new ImmutableConnections("s2_authtls", ImmutableServerProfiles.SERVER_2, BaseConnections.authtls, 17807, "s2_authtls_authuser01_01f", "NV_s2_authtls_authuser012_012f", "s2_authtls_authuser012_012f", "s2_authtls_authuser12_012f", "s2_authtls_authuser3_3f", "NV_s2_authtls_authuser0_0f", "NV_s2_authtls_authuser_f", "NV_s2_authtls_authuser12_012f", "NV_s2_authtls_authuser2_2f", "NV_s2_authtls_authuser01_01f", "NV_s2_authtls_authuser3_3f", "s2_authtls_authuser2_2f", "s2_authtls_authuser0_0f", "s2_authtls_authuser_f");
    public static final ImmutableConnections s1_tcp2f = new ImmutableConnections("s1_tcp2f", ImmutableServerProfiles.SERVER_1, BaseConnections.tcp2f, 17784, "s1_tcp2f_anonuser_2f", "s1_tcp2f_anonuser_2f_A", "s1_tcp2f_anonuser_2f_B", "NV_s1_tcp2f_anonuser_2f", "NV_s1_tcp2f_anonuser_2f_A", "NV_s1_tcp2f_anonuser_2f_B");
    public static final ImmutableConnections sCLI_udp01 = new ImmutableConnections("sCLI_udp01", ImmutableServerProfiles.SERVER_CLI, BaseConnections.udp01, 17884, "NV_sCLI_udp01_anonuser_01f_B", "sCLI_udp01_anonuser_01f_B", "sCLI_udp01_anonuser_01f_A", "sCLI_udp01_anonuser_01f", "NV_sCLI_udp01_anonuser_01f_A", "NV_sCLI_udp01_anonuser_01f");
    public static final ImmutableConnections s2_stcp2f = new ImmutableConnections("s2_stcp2f", ImmutableServerProfiles.SERVER_2, BaseConnections.stcp2f, 17814, "NV_s2_stcp2f_anonuser_2f_A", "NV_s2_stcp2f_anonuser_2f", "NV_s2_stcp2f_anonuser_2f_B", "s2_stcp2f_anonuser_2f_A", "s2_stcp2f_anonuser_2f_B", "s2_stcp2f_anonuser_2f");
    public static final ImmutableConnections sCLI_stcp01 = new ImmutableConnections("sCLI_stcp01", ImmutableServerProfiles.SERVER_CLI, BaseConnections.stcp01, 17874, "NV_sCLI_stcp01_anonuser_01f", "sCLI_stcp01_anonuser_01f_A", "sCLI_stcp01_anonuser_01f_B", "sCLI_stcp01_anonuser_01f", "NV_sCLI_stcp01_anonuser_01f_A", "NV_sCLI_stcp01_anonuser_01f_B");
    public static final ImmutableConnections s1_tcp = new ImmutableConnections("s1_tcp", ImmutableServerProfiles.SERVER_1, BaseConnections.tcp, 17763, "s1_tcp_anonuser_t", "NV_s1_tcp_anonuser_t_A", "NV_s1_tcp_anonuser_t_B", "NV_s1_tcp_anonuser_t", "s1_tcp_anonuser_t_A", "s1_tcp_anonuser_t_B");
    public static final ImmutableConnections sCLI_stcp3 = new ImmutableConnections("sCLI_stcp3", ImmutableServerProfiles.SERVER_CLI, BaseConnections.stcp3, 17894, "sCLI_stcp3_anonuser_3f", "NV_sCLI_stcp3_anonuser_3f_B", "NV_sCLI_stcp3_anonuser_3f_A", "NV_sCLI_stcp3_anonuser_3f", "sCLI_stcp3_anonuser_3f_B", "sCLI_stcp3_anonuser_3f_A");
    public static final ImmutableConnections s1_authssl = new ImmutableConnections("s1_authssl", ImmutableServerProfiles.SERVER_1, BaseConnections.authssl, 17772, "NV_s1_authssl_authuser3_3f", "s1_authssl_authuser2_2f", "NV_s1_authssl_authuser012_012f", "s1_authssl_authuser0_0f", "s1_authssl_authuser_f", "s1_authssl_authuser012_012f", "NV_s1_authssl_authuser0_0f", "NV_s1_authssl_authuser_f", "s1_authssl_authuser01_01f", "NV_s1_authssl_authuser12_012f", "NV_s1_authssl_authuser2_2f", "NV_s1_authssl_authuser01_01f", "s1_authssl_authuser12_012f", "s1_authssl_authuser3_3f");
    public static final ImmutableConnections sCLI_stcp0 = new ImmutableConnections("sCLI_stcp0", ImmutableServerProfiles.SERVER_CLI, BaseConnections.stcp0, 17876, "NV_sCLI_stcp0_anonuser_0f_A", "NV_sCLI_stcp0_anonuser_0f_B", "sCLI_stcp0_anonuser_0f_B", "sCLI_stcp0_anonuser_0f_A", "sCLI_stcp0_anonuser_0f", "NV_sCLI_stcp0_anonuser_0f");
    public static final ImmutableConnections sCLI_tcp01t = new ImmutableConnections("sCLI_tcp01t", ImmutableServerProfiles.SERVER_CLI, BaseConnections.tcp01t, 17882, "NV_sCLI_tcp01t_anonuser_01t", "sCLI_tcp01t_anonuser_01t_A", "NV_sCLI_tcp01t_anonuser_01t_A", "sCLI_tcp01t_anonuser_01t_B", "sCLI_tcp01t_anonuser_01t", "NV_sCLI_tcp01t_anonuser_01t_B");
    public static final ImmutableConnections sCLI_submcast = new ImmutableConnections("sCLI_submcast", ImmutableServerProfiles.SERVER_CLI, BaseConnections.submcast, 17893, "sCLI_submcast_anonuser_t_B", "NV_sCLI_submcast_anonuser_t_B", "NV_sCLI_submcast_anonuser_t_A", "sCLI_submcast_anonuser_t", "NV_sCLI_submcast_anonuser_t", "sCLI_submcast_anonuser_t_A");
    public static final ImmutableConnections s2_mcast01 = new ImmutableConnections("s2_mcast01", ImmutableServerProfiles.SERVER_2, BaseConnections.mcast01, 17821, "NV_s2_mcast01_anonuser_01f_B", "NV_s2_mcast01_anonuser_01f_A", "s2_mcast01_anonuser_01f_A", "NV_s2_mcast01_anonuser_01f", "s2_mcast01_anonuser_01f", "s2_mcast01_anonuser_01f_B");
    public static final ImmutableConnections s2_mcast = new ImmutableConnections("s2_mcast", ImmutableServerProfiles.SERVER_2, BaseConnections.mcast, 17800, "NV_s2_mcast_anonuser_t_B", "NV_s2_mcast_anonuser_t_A", "s2_mcast_anonuser_t_B", "s2_mcast_anonuser_t_A", "NV_s2_mcast_anonuser_t", "s2_mcast_anonuser_t");
    public static final ImmutableConnections sCLI_udp = new ImmutableConnections("sCLI_udp", ImmutableServerProfiles.SERVER_CLI, BaseConnections.udp, 17863, "NV_sCLI_udp_anonuser_t_B", "NV_sCLI_udp_anonuser_t", "sCLI_udp_anonuser_t_A", "sCLI_udp_anonuser_t_B", "NV_sCLI_udp_anonuser_t_A", "sCLI_udp_anonuser_t");
    public static final ImmutableConnections s1_stcp12 = new ImmutableConnections("s1_stcp12", ImmutableServerProfiles.SERVER_1, BaseConnections.stcp12, 17776, "NV_s1_stcp12_anonuser_12f_B", "NV_s1_stcp12_anonuser_12f_A", "s1_stcp12_anonuser_12f", "NV_s1_stcp12_anonuser_12f", "s1_stcp12_anonuser_12f_B", "s1_stcp12_anonuser_12f_A");
    public static final ImmutableConnections s0_authsslA = new ImmutableConnections("s0_authsslA", ImmutableServerProfiles.SERVER_0, BaseConnections.authssl, 17740, "s0_authsslA_authuser3_3f", "NV_s0_authsslA_authuser_f", "NV_s0_authsslA_authuser01_01f", "s0_authsslA_authuser2_2f", "NV_s0_authsslA_authuser12_012f", "s0_authsslA_authuser01_01f", "s0_authsslA_authuser12_012f", "s0_authsslA_authuser0_0f", "s0_authsslA_authuser_f", "s0_authsslA_authuser012_012f", "NV_s0_authsslA_authuser2_2f", "NV_s0_authsslA_authuser012_012f", "NV_s0_authsslA_authuser3_3f", "NV_s0_authsslA_authuser0_0f");
    public static final ImmutableConnections s2_tcp12 = new ImmutableConnections("s2_tcp12", ImmutableServerProfiles.SERVER_2, BaseConnections.tcp12, 17815, "NV_s2_tcp12_anonuser_12f", "s2_tcp12_anonuser_12f", "NV_s2_tcp12_anonuser_12f_A", "s2_tcp12_anonuser_12f_B", "NV_s2_tcp12_anonuser_12f_B", "s2_tcp12_anonuser_12f_A");
    public static final ImmutableConnections sCLI_stcpA = new ImmutableConnections("sCLI_stcpA", ImmutableServerProfiles.SERVER_CLI, BaseConnections.stcp, 17865, "NV_sCLI_stcpA_anonuser_t_A", "NV_sCLI_stcpA_anonuser_t_B", "NV_sCLI_stcpA_anonuser_t", "sCLI_stcpA_anonuser_t", "sCLI_stcpA_anonuser_t_A", "sCLI_stcpA_anonuser_t_B");
    public static final ImmutableConnections s1_stcp = new ImmutableConnections("s1_stcp", ImmutableServerProfiles.SERVER_1, BaseConnections.stcp, 17765, "NV_s1_stcp_anonuser_t_B", "s1_stcp_anonuser_t", "NV_s1_stcp_anonuser_t", "NV_s1_stcp_anonuser_t_A", "s1_stcp_anonuser_t_B", "s1_stcp_anonuser_t_A");
    public static final ImmutableConnections s0_mcast12t = new ImmutableConnections("s0_mcast12t", ImmutableServerProfiles.SERVER_0, BaseConnections.mcast12t, 17756, "s0_mcast12t_anonuser_12t", "NV_s0_mcast12t_anonuser_12t_B", "NV_s0_mcast12t_anonuser_12t_A", "NV_s0_mcast12t_anonuser_12t", "s0_mcast12t_anonuser_12t_A", "s0_mcast12t_anonuser_12t_B");
    public static final ImmutableConnections s1_ssl = new ImmutableConnections("s1_ssl", ImmutableServerProfiles.SERVER_1, BaseConnections.ssl, 17770, "NV_s1_ssl_anonuser_t", "NV_s1_ssl_anonuser_t_B", "NV_s1_ssl_anonuser_t_A", "s1_ssl_anonuser_t", "s1_ssl_anonuser_t_A", "s1_ssl_anonuser_t_B");
    public static final ImmutableConnections s0_mcast3f = new ImmutableConnections("s0_mcast3f", ImmutableServerProfiles.SERVER_0, BaseConnections.mcast3f, 17757, "NV_s0_mcast3f_anonuser_3f_B", "NV_s0_mcast3f_anonuser_3f_A", "NV_s0_mcast3f_anonuser_3f", "s0_mcast3f_anonuser_3f_A", "s0_mcast3f_anonuser_3f", "s0_mcast3f_anonuser_3f_B");
    public static final ImmutableConnections s0_udp3f = new ImmutableConnections("s0_udp3f", ImmutableServerProfiles.SERVER_0, BaseConnections.udp3f, 17754, "NV_s0_udp3f_anonuser_3f_B", "s0_udp3f_anonuser_3f_A", "s0_udp3f_anonuser_3f_B", "NV_s0_udp3f_anonuser_3f", "NV_s0_udp3f_anonuser_3f_A", "s0_udp3f_anonuser_3f");
    public static final ImmutableConnections s0_submcast = new ImmutableConnections("s0_submcast", ImmutableServerProfiles.SERVER_0, BaseConnections.submcast, 17761, "s0_submcast_anonuser_t_B", "NV_s0_submcast_anonuser_t", "NV_s0_submcast_anonuser_t_A", "NV_s0_submcast_anonuser_t_B", "s0_submcast_anonuser_t", "s0_submcast_anonuser_t_A");
    public static final ImmutableConnections s2_stcp3 = new ImmutableConnections("s2_stcp3", ImmutableServerProfiles.SERVER_2, BaseConnections.stcp3, 17828, "NV_s2_stcp3_anonuser_3f", "NV_s2_stcp3_anonuser_3f_A", "s2_stcp3_anonuser_3f_B", "NV_s2_stcp3_anonuser_3f_B", "s2_stcp3_anonuser_3f", "s2_stcp3_anonuser_3f_A");
    public static final ImmutableConnections s2_stcp = new ImmutableConnections("s2_stcp", ImmutableServerProfiles.SERVER_2, BaseConnections.stcp, 17798, "s2_stcp_anonuser_t_A", "NV_s2_stcp_anonuser_t", "s2_stcp_anonuser_t", "s2_stcp_anonuser_t_B", "NV_s2_stcp_anonuser_t_B", "NV_s2_stcp_anonuser_t_A");
    public static final ImmutableConnections s1_tcp12 = new ImmutableConnections("s1_tcp12", ImmutableServerProfiles.SERVER_1, BaseConnections.tcp12, 17782, "NV_s1_tcp12_anonuser_12f_A", "NV_s1_tcp12_anonuser_12f_B", "NV_s1_tcp12_anonuser_12f", "s1_tcp12_anonuser_12f", "s1_tcp12_anonuser_12f_A", "s1_tcp12_anonuser_12f_B");
    public static final ImmutableConnections s2_stcp0 = new ImmutableConnections("s2_stcp0", ImmutableServerProfiles.SERVER_2, BaseConnections.stcp0, 17810, "s2_stcp0_anonuser_0f", "NV_s2_stcp0_anonuser_0f_B", "NV_s2_stcp0_anonuser_0f_A", "NV_s2_stcp0_anonuser_0f", "s2_stcp0_anonuser_0f_A", "s2_stcp0_anonuser_0f_B");
    public static final ImmutableConnections s1_stcp01t = new ImmutableConnections("s1_stcp01t", ImmutableServerProfiles.SERVER_1, BaseConnections.stcp01t, 17780, "s1_stcp01t_anonuser_01t", "NV_s1_stcp01t_anonuser_01t_A", "NV_s1_stcp01t_anonuser_01t", "NV_s1_stcp01t_anonuser_01t_B", "s1_stcp01t_anonuser_01t_A", "s1_stcp01t_anonuser_01t_B");
    public static final ImmutableConnections sCLI_authstcp = new ImmutableConnections("sCLI_authstcp", ImmutableServerProfiles.SERVER_CLI, BaseConnections.authstcp, 17877, "NV_sCLI_authstcp_authuser_f", "NV_sCLI_authstcp_authuser12_012f", "NV_sCLI_authstcp_authuser01_01f", "NV_sCLI_authstcp_authuser2_2f", "NV_sCLI_authstcp_authuser3_3f", "NV_sCLI_authstcp_authuser012_012f", "sCLI_authstcp_authuser3_3f", "sCLI_authstcp_authuser0_0f", "NV_sCLI_authstcp_authuser0_0f", "sCLI_authstcp_authuser2_2f", "sCLI_authstcp_authuser12_012f", "sCLI_authstcp_authuser012_012f", "sCLI_authstcp_authuser_f", "sCLI_authstcp_authuser01_01f");
    public static final ImmutableConnections s0_tcp01t = new ImmutableConnections("s0_tcp01t", ImmutableServerProfiles.SERVER_0, BaseConnections.tcp01t, 17750, "s0_tcp01t_anonuser_01t_B", "s0_tcp01t_anonuser_01t_A", "NV_s0_tcp01t_anonuser_01t_A", "NV_s0_tcp01t_anonuser_01t_B", "s0_tcp01t_anonuser_01t", "NV_s0_tcp01t_anonuser_01t");
    public static final ImmutableConnections s2_udp = new ImmutableConnections("s2_udp", ImmutableServerProfiles.SERVER_2, BaseConnections.udp, 17797, "NV_s2_udp_anonuser_t", "NV_s2_udp_anonuser_t_A", "NV_s2_udp_anonuser_t_B", "s2_udp_anonuser_t", "s2_udp_anonuser_t_A", "s2_udp_anonuser_t_B");
    public static final ImmutableConnections s0_tcp2f = new ImmutableConnections("s0_tcp2f", ImmutableServerProfiles.SERVER_0, BaseConnections.tcp2f, 17751, "NV_s0_tcp2f_anonuser_2f", "NV_s0_tcp2f_anonuser_2f_A", "NV_s0_tcp2f_anonuser_2f_B", "s0_tcp2f_anonuser_2f", "s0_tcp2f_anonuser_2f_A", "s0_tcp2f_anonuser_2f_B");
    public static final ImmutableConnections s0_udp = new ImmutableConnections("s0_udp", ImmutableServerProfiles.SERVER_0, BaseConnections.udp, 17731, "s0_udp_anonuser_t", "s0_udp_anonuser_t_A", "s0_udp_anonuser_t_B", "NV_s0_udp_anonuser_t_B", "NV_s0_udp_anonuser_t", "NV_s0_udp_anonuser_t_A");
    public static final ImmutableConnections s2_mcast12t = new ImmutableConnections("s2_mcast12t", ImmutableServerProfiles.SERVER_2, BaseConnections.mcast12t, 17822, "NV_s2_mcast12t_anonuser_12t_A", "NV_s2_mcast12t_anonuser_12t_B", "s2_mcast12t_anonuser_12t", "NV_s2_mcast12t_anonuser_12t", "s2_mcast12t_anonuser_12t_A", "s2_mcast12t_anonuser_12t_B");
    public static final ImmutableConnections sCLI_saproxy = new ImmutableConnections("sCLI_saproxy", ImmutableServerProfiles.SERVER_CLI, BaseConnections.saproxy, 17867, "NV_sCLI_saproxy_anonuser_t_B", "NV_sCLI_saproxy_anonuser_t_A", "sCLI_saproxy_anonuser_t", "NV_sCLI_saproxy_anonuser_t", "sCLI_saproxy_anonuser_t_B", "sCLI_saproxy_anonuser_t_A");
    public static final ImmutableConnections s2_stcpA = new ImmutableConnections("s2_stcpA", ImmutableServerProfiles.SERVER_2, BaseConnections.stcp, 17799, "s2_stcpA_anonuser_t_B", "s2_stcpA_anonuser_t_A", "s2_stcpA_anonuser_t", "NV_s2_stcpA_anonuser_t", "NV_s2_stcpA_anonuser_t_B", "NV_s2_stcpA_anonuser_t_A");
    public static final ImmutableConnections sCLI_mcast01 = new ImmutableConnections("sCLI_mcast01", ImmutableServerProfiles.SERVER_CLI, BaseConnections.mcast01, 17887, "sCLI_mcast01_anonuser_01f", "sCLI_mcast01_anonuser_01f_A", "sCLI_mcast01_anonuser_01f_B", "NV_sCLI_mcast01_anonuser_01f", "NV_sCLI_mcast01_anonuser_01f_A", "NV_sCLI_mcast01_anonuser_01f_B");
    public static final ImmutableConnections sCLI_mcast12t = new ImmutableConnections("sCLI_mcast12t", ImmutableServerProfiles.SERVER_CLI, BaseConnections.mcast12t, 17888, "sCLI_mcast12t_anonuser_12t", "sCLI_mcast12t_anonuser_12t_A", "sCLI_mcast12t_anonuser_12t_B", "NV_sCLI_mcast12t_anonuser_12t_A", "NV_sCLI_mcast12t_anonuser_12t_B", "NV_sCLI_mcast12t_anonuser_12t");
    public static final ImmutableConnections s0_authssl = new ImmutableConnections("s0_authssl", ImmutableServerProfiles.SERVER_0, BaseConnections.authssl, 17739, "s0_authssl_authuser12_012f", "NV_s0_authssl_authuser012_012f", "s0_authssl_authuser3_3f", "s0_authssl_authuser_f", "NV_s0_authssl_authuser3_3f", "s0_authssl_authuser012_012f", "NV_s0_authssl_authuser01_01f", "NV_s0_authssl_authuser2_2f", "s0_authssl_authuser01_01f", "NV_s0_authssl_authuser0_0f", "NV_s0_authssl_authuser_f", "s0_authssl_authuser2_2f", "NV_s0_authssl_authuser12_012f", "s0_authssl_authuser0_0f");
    public static final ImmutableConnections s2_authsslA = new ImmutableConnections("s2_authsslA", ImmutableServerProfiles.SERVER_2, BaseConnections.authssl, 17806, "s2_authsslA_authuser3_3f", "s2_authsslA_authuser2_2f", "NV_s2_authsslA_authuser01_01f", "NV_s2_authsslA_authuser0_0f", "s2_authsslA_authuser12_012f", "NV_s2_authsslA_authuser12_012f", "s2_authsslA_authuser01_01f", "s2_authsslA_authuser0_0f", "NV_s2_authsslA_authuser_f", "NV_s2_authsslA_authuser012_012f", "NV_s2_authsslA_authuser2_2f", "s2_authsslA_authuser_f", "NV_s2_authsslA_authuser3_3f", "s2_authsslA_authuser012_012f");
    public static final ImmutableConnections sCLI_subudp = new ImmutableConnections("sCLI_subudp", ImmutableServerProfiles.SERVER_CLI, BaseConnections.subudp, 17891, "sCLI_subudp_anonuser_t_A", "sCLI_subudp_anonuser_t_B", "sCLI_subudp_anonuser_t", "NV_sCLI_subudp_anonuser_t_B", "NV_sCLI_subudp_anonuser_t", "NV_sCLI_subudp_anonuser_t_A");
    public static final ImmutableConnections s0_saproxyA = new ImmutableConnections("s0_saproxyA", ImmutableServerProfiles.SERVER_0, BaseConnections.saproxy, 17736, "s0_saproxyA_anonuser_t", "s0_saproxyA_anonuser_t_B", "s0_saproxyA_anonuser_t_A", "NV_s0_saproxyA_anonuser_t", "NV_s0_saproxyA_anonuser_t_B", "NV_s0_saproxyA_anonuser_t_A");
    public static final ImmutableConnections s0_subudp = new ImmutableConnections("s0_subudp", ImmutableServerProfiles.SERVER_0, BaseConnections.subudp, 17759, "s0_subudp_anonuser_t_B", "s0_subudp_anonuser_t", "s0_subudp_anonuser_t_A", "NV_s0_subudp_anonuser_t_A", "NV_s0_subudp_anonuser_t_B", "NV_s0_subudp_anonuser_t");
    public static final ImmutableConnections sCLI_udp12t = new ImmutableConnections("sCLI_udp12t", ImmutableServerProfiles.SERVER_CLI, BaseConnections.udp12t, 17885, "sCLI_udp12t_anonuser_12t", "NV_sCLI_udp12t_anonuser_12t_B", "sCLI_udp12t_anonuser_12t_B", "sCLI_udp12t_anonuser_12t_A", "NV_sCLI_udp12t_anonuser_12t", "NV_sCLI_udp12t_anonuser_12t_A");
    public static final ImmutableConnections sCLI_tcp12 = new ImmutableConnections("sCLI_tcp12", ImmutableServerProfiles.SERVER_CLI, BaseConnections.tcp12, 17881, "NV_sCLI_tcp12_anonuser_12f_B", "sCLI_tcp12_anonuser_12f_B", "sCLI_tcp12_anonuser_12f_A", "NV_sCLI_tcp12_anonuser_12f", "NV_sCLI_tcp12_anonuser_12f_A", "sCLI_tcp12_anonuser_12f");
    public static final ImmutableConnections s0_stcp12 = new ImmutableConnections("s0_stcp12", ImmutableServerProfiles.SERVER_0, BaseConnections.stcp12, 17743, "NV_s0_stcp12_anonuser_12f_B", "NV_s0_stcp12_anonuser_12f_A", "NV_s0_stcp12_anonuser_12f", "s0_stcp12_anonuser_12f", "s0_stcp12_anonuser_12f_A", "s0_stcp12_anonuser_12f_B");
    public static final ImmutableConnections s2_subudp = new ImmutableConnections("s2_subudp", ImmutableServerProfiles.SERVER_2, BaseConnections.subudp, 17825, "s2_subudp_anonuser_t_B", "s2_subudp_anonuser_t_A", "NV_s2_subudp_anonuser_t_A", "s2_subudp_anonuser_t", "NV_s2_subudp_anonuser_t_B", "NV_s2_subudp_anonuser_t");
    public static final ImmutableConnections s1_authstcp = new ImmutableConnections("s1_authstcp", ImmutableServerProfiles.SERVER_1, BaseConnections.authstcp, 17778, "NV_s1_authstcp_authuser012_012f", "NV_s1_authstcp_authuser0_0f", "s1_authstcp_authuser01_01f", "NV_s1_authstcp_authuser12_012f", "s1_authstcp_authuser12_012f", "s1_authstcp_authuser_f", "NV_s1_authstcp_authuser3_3f", "s1_authstcp_authuser2_2f", "NV_s1_authstcp_authuser2_2f", "s1_authstcp_authuser3_3f", "NV_s1_authstcp_authuser01_01f", "NV_s1_authstcp_authuser_f", "s1_authstcp_authuser0_0f", "s1_authstcp_authuser012_012f");
    public static final ImmutableConnections sCLI_mcast = new ImmutableConnections("sCLI_mcast", ImmutableServerProfiles.SERVER_CLI, BaseConnections.mcast, 17866, "NV_sCLI_mcast_anonuser_t", "sCLI_mcast_anonuser_t", "sCLI_mcast_anonuser_t_A", "NV_sCLI_mcast_anonuser_t_A", "NV_sCLI_mcast_anonuser_t_B", "sCLI_mcast_anonuser_t_B");
    public static final ImmutableConnections sCLI_authsslA = new ImmutableConnections("sCLI_authsslA", ImmutableServerProfiles.SERVER_CLI, BaseConnections.authssl, 17872, "sCLI_authsslA_authuser01_01f", "NV_sCLI_authsslA_authuser01_01f", "NV_sCLI_authsslA_authuser12_012f", "sCLI_authsslA_authuser_f", "sCLI_authsslA_authuser012_012f", "NV_sCLI_authsslA_authuser0_0f", "NV_sCLI_authsslA_authuser2_2f", "NV_sCLI_authsslA_authuser3_3f", "NV_sCLI_authsslA_authuser_f", "sCLI_authsslA_authuser3_3f", "sCLI_authsslA_authuser12_012f", "sCLI_authsslA_authuser2_2f", "sCLI_authsslA_authuser0_0f", "NV_sCLI_authsslA_authuser012_012f");
    public static final ImmutableConnections s2_authstcp = new ImmutableConnections("s2_authstcp", ImmutableServerProfiles.SERVER_2, BaseConnections.authstcp, 17811, "NV_s2_authstcp_authuser01_01f", "s2_authstcp_authuser12_012f", "s2_authstcp_authuser0_0f", "s2_authstcp_authuser012_012f", "s2_authstcp_authuser01_01f", "NV_s2_authstcp_authuser012_012f", "NV_s2_authstcp_authuser0_0f", "s2_authstcp_authuser3_3f", "s2_authstcp_authuser2_2f", "NV_s2_authstcp_authuser3_3f", "NV_s2_authstcp_authuser12_012f", "s2_authstcp_authuser_f", "NV_s2_authstcp_authuser2_2f", "NV_s2_authstcp_authuser_f");
    public static final ImmutableConnections s0_tcp = new ImmutableConnections("s0_tcp", ImmutableServerProfiles.SERVER_0, BaseConnections.tcp, 17730, "s0_tcp_anonuser_t", "s0_tcp_anonuser_t_B", "s0_tcp_anonuser_t_A", "NV_s0_tcp_anonuser_t_B", "NV_s0_tcp_anonuser_t_A", "NV_s0_tcp_anonuser_t");
    public static final ImmutableConnections s1_tcp01t = new ImmutableConnections("s1_tcp01t", ImmutableServerProfiles.SERVER_1, BaseConnections.tcp01t, 17783, "s1_tcp01t_anonuser_01t_B", "s1_tcp01t_anonuser_01t", "s1_tcp01t_anonuser_01t_A", "NV_s1_tcp01t_anonuser_01t", "NV_s1_tcp01t_anonuser_01t_A", "NV_s1_tcp01t_anonuser_01t_B");
    public static final ImmutableConnections s1_mcast3f = new ImmutableConnections("s1_mcast3f", ImmutableServerProfiles.SERVER_1, BaseConnections.mcast3f, 17790, "NV_s1_mcast3f_anonuser_3f_B", "NV_s1_mcast3f_anonuser_3f", "NV_s1_mcast3f_anonuser_3f_A", "s1_mcast3f_anonuser_3f", "s1_mcast3f_anonuser_3f_B", "s1_mcast3f_anonuser_3f_A");
    public static final ImmutableConnections sCLI_subtcp = new ImmutableConnections("sCLI_subtcp", ImmutableServerProfiles.SERVER_CLI, BaseConnections.subtcp, 17890, "sCLI_subtcp_anonuser_t_A", "sCLI_subtcp_anonuser_t", "sCLI_subtcp_anonuser_t_B", "NV_sCLI_subtcp_anonuser_t", "NV_sCLI_subtcp_anonuser_t_B", "NV_sCLI_subtcp_anonuser_t_A");
    public static final ImmutableConnections s0_stcp = new ImmutableConnections("s0_stcp", ImmutableServerProfiles.SERVER_0, BaseConnections.stcp, 17732, "NV_s0_stcp_anonuser_t_B", "NV_s0_stcp_anonuser_t_A", "s0_stcp_anonuser_t_A", "s0_stcp_anonuser_t_B", "s0_stcp_anonuser_t", "NV_s0_stcp_anonuser_t");
    public static final ImmutableConnections s0_udp12t = new ImmutableConnections("s0_udp12t", ImmutableServerProfiles.SERVER_0, BaseConnections.udp12t, 17753, "s0_udp12t_anonuser_12t", "NV_s0_udp12t_anonuser_12t_A", "NV_s0_udp12t_anonuser_12t", "s0_udp12t_anonuser_12t_A", "s0_udp12t_anonuser_12t_B", "NV_s0_udp12t_anonuser_12t_B");
    public static final ImmutableConnections s2_udp12t = new ImmutableConnections("s2_udp12t", ImmutableServerProfiles.SERVER_2, BaseConnections.udp12t, 17819, "s2_udp12t_anonuser_12t", "NV_s2_udp12t_anonuser_12t_B", "NV_s2_udp12t_anonuser_12t", "NV_s2_udp12t_anonuser_12t_A", "s2_udp12t_anonuser_12t_B", "s2_udp12t_anonuser_12t_A");
    public static final ImmutableConnections s2_stcp12 = new ImmutableConnections("s2_stcp12", ImmutableServerProfiles.SERVER_2, BaseConnections.stcp12, 17809, "s2_stcp12_anonuser_12f", "NV_s2_stcp12_anonuser_12f", "s2_stcp12_anonuser_12f_B", "s2_stcp12_anonuser_12f_A", "NV_s2_stcp12_anonuser_12f_A", "NV_s2_stcp12_anonuser_12f_B");
    public static final ImmutableConnections s2_udp01 = new ImmutableConnections("s2_udp01", ImmutableServerProfiles.SERVER_2, BaseConnections.udp01, 17818, "s2_udp01_anonuser_01f_A", "s2_udp01_anonuser_01f_B", "NV_s2_udp01_anonuser_01f", "NV_s2_udp01_anonuser_01f_B", "s2_udp01_anonuser_01f", "NV_s2_udp01_anonuser_01f_A");
    public static final ImmutableConnections sCLI_stcp12 = new ImmutableConnections("sCLI_stcp12", ImmutableServerProfiles.SERVER_CLI, BaseConnections.stcp12, 17875, "sCLI_stcp12_anonuser_12f", "NV_sCLI_stcp12_anonuser_12f_B", "NV_sCLI_stcp12_anonuser_12f_A", "sCLI_stcp12_anonuser_12f_B", "NV_sCLI_stcp12_anonuser_12f", "sCLI_stcp12_anonuser_12f_A");
    public static final ImmutableConnections s0_substcp = new ImmutableConnections("s0_substcp", ImmutableServerProfiles.SERVER_0, BaseConnections.substcp, 17760, "NV_s0_substcp_anonuser_t_B", "NV_s0_substcp_anonuser_t_A", "s0_substcp_anonuser_t_A", "s0_substcp_anonuser_t_B", "s0_substcp_anonuser_t", "NV_s0_substcp_anonuser_t");
    public static final ImmutableConnections s1_subtcp = new ImmutableConnections("s1_subtcp", ImmutableServerProfiles.SERVER_1, BaseConnections.subtcp, 17791, "s1_subtcp_anonuser_t", "s1_subtcp_anonuser_t_B", "s1_subtcp_anonuser_t_A", "NV_s1_subtcp_anonuser_t_A", "NV_s1_subtcp_anonuser_t", "NV_s1_subtcp_anonuser_t_B");
    public static final ImmutableConnections sCLI_authtls = new ImmutableConnections("sCLI_authtls", ImmutableServerProfiles.SERVER_CLI, BaseConnections.authtls, 17873, "NV_sCLI_authtls_authuser2_2f", "sCLI_authtls_authuser01_01f", "NV_sCLI_authtls_authuser3_3f", "NV_sCLI_authtls_authuser_f", "sCLI_authtls_authuser_f", "NV_sCLI_authtls_authuser0_0f", "sCLI_authtls_authuser12_012f", "NV_sCLI_authtls_authuser12_012f", "sCLI_authtls_authuser3_3f", "sCLI_authtls_authuser2_2f", "sCLI_authtls_authuser012_012f", "NV_sCLI_authtls_authuser012_012f", "sCLI_authtls_authuser0_0f", "NV_sCLI_authtls_authuser01_01f");
    public static final ImmutableConnections s1_authsslA = new ImmutableConnections("s1_authsslA", ImmutableServerProfiles.SERVER_1, BaseConnections.authssl, 17773, "s1_authsslA_authuser012_012f", "NV_s1_authsslA_authuser012_012f", "NV_s1_authsslA_authuser2_2f", "NV_s1_authsslA_authuser12_012f", "NV_s1_authsslA_authuser3_3f", "NV_s1_authsslA_authuser0_0f", "NV_s1_authsslA_authuser01_01f", "s1_authsslA_authuser2_2f", "s1_authsslA_authuser3_3f", "s1_authsslA_authuser01_01f", "s1_authsslA_authuser0_0f", "s1_authsslA_authuser_f", "s1_authsslA_authuser12_012f", "NV_s1_authsslA_authuser_f");
    public static final ImmutableConnections s2_subtcp = new ImmutableConnections("s2_subtcp", ImmutableServerProfiles.SERVER_2, BaseConnections.subtcp, 17824, "NV_s2_subtcp_anonuser_t_B", "NV_s2_subtcp_anonuser_t_A", "s2_subtcp_anonuser_t", "NV_s2_subtcp_anonuser_t", "s2_subtcp_anonuser_t_A", "s2_subtcp_anonuser_t_B");
    public static final ImmutableConnections s2_ssl = new ImmutableConnections("s2_ssl", ImmutableServerProfiles.SERVER_2, BaseConnections.ssl, 17803, "s2_ssl_anonuser_t_A", "s2_ssl_anonuser_t_B", "s2_ssl_anonuser_t", "NV_s2_ssl_anonuser_t", "NV_s2_ssl_anonuser_t_A", "NV_s2_ssl_anonuser_t_B");
    public static final ImmutableConnections s1_stcpA = new ImmutableConnections("s1_stcpA", ImmutableServerProfiles.SERVER_1, BaseConnections.stcp, 17766, "s1_stcpA_anonuser_t_A", "s1_stcpA_anonuser_t", "s1_stcpA_anonuser_t_B", "NV_s1_stcpA_anonuser_t", "NV_s1_stcpA_anonuser_t_B", "NV_s1_stcpA_anonuser_t_A");
    public static final ImmutableConnections s0_stcp2f = new ImmutableConnections("s0_stcp2f", ImmutableServerProfiles.SERVER_0, BaseConnections.stcp2f, 17748, "NV_s0_stcp2f_anonuser_2f_B", "s0_stcp2f_anonuser_2f", "NV_s0_stcp2f_anonuser_2f_A", "s0_stcp2f_anonuser_2f_A", "s0_stcp2f_anonuser_2f_B", "NV_s0_stcp2f_anonuser_2f");
    public static final ImmutableConnections sCLI_authssl = new ImmutableConnections("sCLI_authssl", ImmutableServerProfiles.SERVER_CLI, BaseConnections.authssl, 17871, "NV_sCLI_authssl_authuser012_012f", "sCLI_authssl_authuser0_0f", "NV_sCLI_authssl_authuser01_01f", "NV_sCLI_authssl_authuser12_012f", "sCLI_authssl_authuser2_2f", "sCLI_authssl_authuser01_01f", "sCLI_authssl_authuser12_012f", "NV_sCLI_authssl_authuser_f", "sCLI_authssl_authuser012_012f", "NV_sCLI_authssl_authuser3_3f", "NV_sCLI_authssl_authuser2_2f", "sCLI_authssl_authuser3_3f", "NV_sCLI_authssl_authuser0_0f", "sCLI_authssl_authuser_f");
    public static final ImmutableConnections s1_stcp2f = new ImmutableConnections("s1_stcp2f", ImmutableServerProfiles.SERVER_1, BaseConnections.stcp2f, 17781, "s1_stcp2f_anonuser_2f", "NV_s1_stcp2f_anonuser_2f_B", "s1_stcp2f_anonuser_2f_B", "s1_stcp2f_anonuser_2f_A", "NV_s1_stcp2f_anonuser_2f", "NV_s1_stcp2f_anonuser_2f_A");
    public static final ImmutableConnections s1_udp01 = new ImmutableConnections("s1_udp01", ImmutableServerProfiles.SERVER_1, BaseConnections.udp01, 17785, "s1_udp01_anonuser_01f", "NV_s1_udp01_anonuser_01f", "s1_udp01_anonuser_01f_B", "s1_udp01_anonuser_01f_A", "NV_s1_udp01_anonuser_01f_A", "NV_s1_udp01_anonuser_01f_B");
    public static final ImmutableConnections s2_stcp01t = new ImmutableConnections("s2_stcp01t", ImmutableServerProfiles.SERVER_2, BaseConnections.stcp01t, 17813, "NV_s2_stcp01t_anonuser_01t", "s2_stcp01t_anonuser_01t_A", "NV_s2_stcp01t_anonuser_01t_B", "NV_s2_stcp01t_anonuser_01t_A", "s2_stcp01t_anonuser_01t_B", "s2_stcp01t_anonuser_01t");
    public static final ImmutableConnections s0_tls = new ImmutableConnections("s0_tls", ImmutableServerProfiles.SERVER_0, BaseConnections.tls, 17738, "NV_s0_tls_anonuser_t_B", "NV_s0_tls_anonuser_t_A", "NV_s0_tls_anonuser_t", "s0_tls_anonuser_t_B", "s0_tls_anonuser_t_A", "s0_tls_anonuser_t");
    public static final ImmutableConnections s2_stcp01 = new ImmutableConnections("s2_stcp01", ImmutableServerProfiles.SERVER_2, BaseConnections.stcp01, 17808, "s2_stcp01_anonuser_01f_A", "NV_s2_stcp01_anonuser_01f_A", "s2_stcp01_anonuser_01f", "NV_s2_stcp01_anonuser_01f_B", "s2_stcp01_anonuser_01f_B", "NV_s2_stcp01_anonuser_01f");
    public static final ImmutableConnections s1_stcp3 = new ImmutableConnections("s1_stcp3", ImmutableServerProfiles.SERVER_1, BaseConnections.stcp3, 17795, "NV_s1_stcp3_anonuser_3f_A", "s1_stcp3_anonuser_3f", "NV_s1_stcp3_anonuser_3f_B", "s1_stcp3_anonuser_3f_A", "s1_stcp3_anonuser_3f_B", "NV_s1_stcp3_anonuser_3f");
    public static final ImmutableConnections s1_stcp0 = new ImmutableConnections("s1_stcp0", ImmutableServerProfiles.SERVER_1, BaseConnections.stcp0, 17777, "NV_s1_stcp0_anonuser_0f", "s1_stcp0_anonuser_0f_A", "s1_stcp0_anonuser_0f_B", "s1_stcp0_anonuser_0f", "NV_s1_stcp0_anonuser_0f_B", "NV_s1_stcp0_anonuser_0f_A");
    public static final ImmutableConnections s0_tcp12 = new ImmutableConnections("s0_tcp12", ImmutableServerProfiles.SERVER_0, BaseConnections.tcp12, 17749, "s0_tcp12_anonuser_12f_B", "s0_tcp12_anonuser_12f_A", "s0_tcp12_anonuser_12f", "NV_s0_tcp12_anonuser_12f_A", "NV_s0_tcp12_anonuser_12f_B", "NV_s0_tcp12_anonuser_12f");
    public static final ImmutableConnections s1_mcast12t = new ImmutableConnections("s1_mcast12t", ImmutableServerProfiles.SERVER_1, BaseConnections.mcast12t, 17789, "s1_mcast12t_anonuser_12t", "NV_s1_mcast12t_anonuser_12t_B", "NV_s1_mcast12t_anonuser_12t_A", "NV_s1_mcast12t_anonuser_12t", "s1_mcast12t_anonuser_12t_A", "s1_mcast12t_anonuser_12t_B");
    public static final ImmutableConnections s1_subudp = new ImmutableConnections("s1_subudp", ImmutableServerProfiles.SERVER_1, BaseConnections.subudp, 17792, "NV_s1_subudp_anonuser_t", "s1_subudp_anonuser_t_B", "s1_subudp_anonuser_t", "NV_s1_subudp_anonuser_t_B", "NV_s1_subudp_anonuser_t_A", "s1_subudp_anonuser_t_A");
    public static final ImmutableConnections s0_ssl = new ImmutableConnections("s0_ssl", ImmutableServerProfiles.SERVER_0, BaseConnections.ssl, 17737, "NV_s0_ssl_anonuser_t_A", "NV_s0_ssl_anonuser_t_B", "NV_s0_ssl_anonuser_t", "s0_ssl_anonuser_t", "s0_ssl_anonuser_t_B", "s0_ssl_anonuser_t_A");
    public static final ImmutableConnections s0_authtls = new ImmutableConnections("s0_authtls", ImmutableServerProfiles.SERVER_0, BaseConnections.authtls, 17741, "s0_authtls_authuser12_012f", "NV_s0_authtls_authuser01_01f", "NV_s0_authtls_authuser_f", "s0_authtls_authuser01_01f", "s0_authtls_authuser0_0f", "s0_authtls_authuser3_3f", "s0_authtls_authuser2_2f", "NV_s0_authtls_authuser12_012f", "s0_authtls_authuser012_012f", "NV_s0_authtls_authuser2_2f", "NV_s0_authtls_authuser3_3f", "NV_s0_authtls_authuser012_012f", "s0_authtls_authuser_f", "NV_s0_authtls_authuser0_0f");
    public static final ImmutableConnections sCLI_stcp = new ImmutableConnections("sCLI_stcp", ImmutableServerProfiles.SERVER_CLI, BaseConnections.stcp, 17864, "NV_sCLI_stcp_anonuser_t_B", "NV_sCLI_stcp_anonuser_t_A", "sCLI_stcp_anonuser_t_B", "sCLI_stcp_anonuser_t_A", "NV_sCLI_stcp_anonuser_t", "sCLI_stcp_anonuser_t");
    public static final ImmutableConnections s1_authstcpA = new ImmutableConnections("s1_authstcpA", ImmutableServerProfiles.SERVER_1, BaseConnections.authstcp, 17779, "NV_s1_authstcpA_authuser12_012f", "s1_authstcpA_authuser01_01f", "s1_authstcpA_authuser_f", "s1_authstcpA_authuser12_012f", "s1_authstcpA_authuser012_012f", "NV_s1_authstcpA_authuser_f", "NV_s1_authstcpA_authuser01_01f", "NV_s1_authstcpA_authuser012_012f", "NV_s1_authstcpA_authuser3_3f", "s1_authstcpA_authuser0_0f", "NV_s1_authstcpA_authuser2_2f", "NV_s1_authstcpA_authuser0_0f", "s1_authstcpA_authuser2_2f", "s1_authstcpA_authuser3_3f");
    public static final ImmutableConnections s2_tcp = new ImmutableConnections("s2_tcp", ImmutableServerProfiles.SERVER_2, BaseConnections.tcp, 17796, "NV_s2_tcp_anonuser_t_B", "NV_s2_tcp_anonuser_t_A", "s2_tcp_anonuser_t", "NV_s2_tcp_anonuser_t", "s2_tcp_anonuser_t_A", "s2_tcp_anonuser_t_B");
    public static final ImmutableConnections s0_mcast01 = new ImmutableConnections("s0_mcast01", ImmutableServerProfiles.SERVER_0, BaseConnections.mcast01, 17755, "NV_s0_mcast01_anonuser_01f", "NV_s0_mcast01_anonuser_01f_A", "NV_s0_mcast01_anonuser_01f_B", "s0_mcast01_anonuser_01f", "s0_mcast01_anonuser_01f_A", "s0_mcast01_anonuser_01f_B");
    public static final ImmutableConnections s0_udp01 = new ImmutableConnections("s0_udp01", ImmutableServerProfiles.SERVER_0, BaseConnections.udp01, 17752, "s0_udp01_anonuser_01f", "s0_udp01_anonuser_01f_B", "s0_udp01_anonuser_01f_A", "NV_s0_udp01_anonuser_01f", "NV_s0_udp01_anonuser_01f_A", "NV_s0_udp01_anonuser_01f_B");
    public static final ImmutableConnections sCLI_stcp2f = new ImmutableConnections("sCLI_stcp2f", ImmutableServerProfiles.SERVER_CLI, BaseConnections.stcp2f, 17880, "sCLI_stcp2f_anonuser_2f_B", "NV_sCLI_stcp2f_anonuser_2f", "sCLI_stcp2f_anonuser_2f_A", "NV_sCLI_stcp2f_anonuser_2f_B", "sCLI_stcp2f_anonuser_2f", "NV_sCLI_stcp2f_anonuser_2f_A");
    public static final ImmutableConnections s1_udp12t = new ImmutableConnections("s1_udp12t", ImmutableServerProfiles.SERVER_1, BaseConnections.udp12t, 17786, "NV_s1_udp12t_anonuser_12t_B", "NV_s1_udp12t_anonuser_12t_A", "s1_udp12t_anonuser_12t_B", "s1_udp12t_anonuser_12t_A", "s1_udp12t_anonuser_12t", "NV_s1_udp12t_anonuser_12t");
    public static final ImmutableConnections s2_mcast3f = new ImmutableConnections("s2_mcast3f", ImmutableServerProfiles.SERVER_2, BaseConnections.mcast3f, 17823, "NV_s2_mcast3f_anonuser_3f_A", "NV_s2_mcast3f_anonuser_3f_B", "s2_mcast3f_anonuser_3f_A", "NV_s2_mcast3f_anonuser_3f", "s2_mcast3f_anonuser_3f_B", "s2_mcast3f_anonuser_3f");
    public static final ImmutableConnections s2_tls = new ImmutableConnections("s2_tls", ImmutableServerProfiles.SERVER_2, BaseConnections.tls, 17804, "s2_tls_anonuser_t", "s2_tls_anonuser_t_B", "s2_tls_anonuser_t_A", "NV_s2_tls_anonuser_t", "NV_s2_tls_anonuser_t_A", "NV_s2_tls_anonuser_t_B");
    public static final ImmutableConnections s2_tcp01t = new ImmutableConnections("s2_tcp01t", ImmutableServerProfiles.SERVER_2, BaseConnections.tcp01t, 17816, "s2_tcp01t_anonuser_01t_A", "s2_tcp01t_anonuser_01t", "s2_tcp01t_anonuser_01t_B", "NV_s2_tcp01t_anonuser_01t", "NV_s2_tcp01t_anonuser_01t_A", "NV_s2_tcp01t_anonuser_01t_B");
    public static final ImmutableConnections s2_saproxyA = new ImmutableConnections("s2_saproxyA", ImmutableServerProfiles.SERVER_2, BaseConnections.saproxy, 17802, "NV_s2_saproxyA_anonuser_t_A", "s2_saproxyA_anonuser_t", "NV_s2_saproxyA_anonuser_t", "NV_s2_saproxyA_anonuser_t_B", "s2_saproxyA_anonuser_t_A", "s2_saproxyA_anonuser_t_B");
    public static final ImmutableConnections sCLI_authstcpA = new ImmutableConnections("sCLI_authstcpA", ImmutableServerProfiles.SERVER_CLI, BaseConnections.authstcp, 17878, "sCLI_authstcpA_authuser012_012f", "sCLI_authstcpA_authuser_f", "sCLI_authstcpA_authuser01_01f", "NV_sCLI_authstcpA_authuser12_012f", "NV_sCLI_authstcpA_authuser01_01f", "NV_sCLI_authstcpA_authuser012_012f", "sCLI_authstcpA_authuser12_012f", "NV_sCLI_authstcpA_authuser3_3f", "NV_sCLI_authstcpA_authuser2_2f", "sCLI_authstcpA_authuser3_3f", "sCLI_authstcpA_authuser0_0f", "sCLI_authstcpA_authuser2_2f", "NV_sCLI_authstcpA_authuser0_0f", "NV_sCLI_authstcpA_authuser_f");
    public static final ImmutableConnections s1_substcp = new ImmutableConnections("s1_substcp", ImmutableServerProfiles.SERVER_1, BaseConnections.substcp, 17793, "NV_s1_substcp_anonuser_t_B", "NV_s1_substcp_anonuser_t_A", "s1_substcp_anonuser_t", "s1_substcp_anonuser_t_A", "NV_s1_substcp_anonuser_t", "s1_substcp_anonuser_t_B");
    public static final ImmutableConnections s2_saproxy = new ImmutableConnections("s2_saproxy", ImmutableServerProfiles.SERVER_2, BaseConnections.saproxy, 17801, "s2_saproxy_anonuser_t", "NV_s2_saproxy_anonuser_t", "s2_saproxy_anonuser_t_B", "s2_saproxy_anonuser_t_A", "NV_s2_saproxy_anonuser_t_A", "NV_s2_saproxy_anonuser_t_B");
    
    // Data feed connections
    public static final ImmutableConnections s0_stcp12_data = new ImmutableConnections("s0_stcp12_data", ImmutableServerProfiles.SERVER_0, BaseConnections.stcp12_data, 18743, "NV_s0_stcp12_anonuser_12f_B", "NV_s0_stcp12_anonuser_12f_A", "NV_s0_stcp12_anonuser_12f", "s0_stcp12_anonuser_12f", "s0_stcp12_anonuser_12f_A", "s0_stcp12_anonuser_12f_B");
    public static final ImmutableConnections s0_udp12t_data = new ImmutableConnections("s0_udp12t_data", ImmutableServerProfiles.SERVER_0, BaseConnections.udp12t_data, 18753, "s0_udp12t_anonuser_12t", "NV_s0_udp12t_anonuser_12t_A", "NV_s0_udp12t_anonuser_12t", "s0_udp12t_anonuser_12t_A", "s0_udp12t_anonuser_12t_B", "NV_s0_udp12t_anonuser_12t_B");
    public static final ImmutableConnections s0_stcp_data = new ImmutableConnections("s0_stcp_data", ImmutableServerProfiles.SERVER_0, BaseConnections.stcp_data, 18732, "NV_s0_stcp_anonuser_t_B", "NV_s0_stcp_anonuser_t_A", "s0_stcp_anonuser_t_A", "s0_stcp_anonuser_t_B", "s0_stcp_anonuser_t", "NV_s0_stcp_anonuser_t");
    public static final ImmutableConnections s0_tcp01t_data = new ImmutableConnections("s0_tcp01t_data", ImmutableServerProfiles.SERVER_0, BaseConnections.tcp01t_data, 18750, "s0_tcp01t_anonuser_01t_B", "s0_tcp01t_anonuser_01t_A", "NV_s0_tcp01t_anonuser_01t_A", "NV_s0_tcp01t_anonuser_01t_B", "s0_tcp01t_anonuser_01t", "NV_s0_tcp01t_anonuser_01t");
    public static final ImmutableConnections s0_stcp3_data = new ImmutableConnections("s0_stcp3_data", ImmutableServerProfiles.SERVER_0, BaseConnections.stcp3_data, 18762, "NV_s0_stcp3_anonuser_3f", "s0_stcp3_anonuser_3f", "NV_s0_stcp3_anonuser_3f_A", "s0_stcp3_anonuser_3f_B", "NV_s0_stcp3_anonuser_3f_B", "s0_stcp3_anonuser_3f_A");
    public static final ImmutableConnections s0_authstcpA_data = new ImmutableConnections("s0_authstcpA_data", ImmutableServerProfiles.SERVER_0, BaseConnections.authstcp_data, 18758, "NV_s0_authstcpA_authuser012_012f", "NV_s0_authstcpA_authuser01_01f", "s0_authstcpA_authuser01_01f", "NV_s0_authstcpA_authuser0_0f", "NV_s0_authstcpA_authuser_f", "NV_s0_authstcpA_authuser12_012f", "NV_s0_authstcpA_authuser2_2f", "s0_authstcpA_authuser_f", "s0_authstcpA_authuser12_012f", "s0_authstcpA_authuser012_012f", "NV_s0_authstcpA_authuser3_3f", "s0_authstcpA_authuser0_0f", "s0_authstcpA_authuser3_3f", "s0_authstcpA_authuser2_2f");
    public static final ImmutableConnections s0_authstcp_data = new ImmutableConnections("s0_authstcp_data", ImmutableServerProfiles.SERVER_0, BaseConnections.authstcp_data, 18746, "NV_s0_authstcp_authuser01_01f", "NV_s0_authstcp_authuser12_012f", "NV_s0_authstcp_authuser_f", "s0_authstcp_authuser_f", "s0_authstcp_authuser012_012f", "s0_authstcp_authuser01_01f", "NV_s0_authstcp_authuser0_0f", "NV_s0_authstcp_authuser2_2f", "NV_s0_authstcp_authuser3_3f", "NV_s0_authstcp_authuser012_012f", "s0_authstcp_authuser12_012f", "s0_authstcp_authuser0_0f", "s0_authstcp_authuser3_3f", "s0_authstcp_authuser2_2f");
    public static final ImmutableConnections s0_udp3f_data = new ImmutableConnections("s0_udp3f_data", ImmutableServerProfiles.SERVER_0, BaseConnections.udp3f_data, 18754, "NV_s0_udp3f_anonuser_3f_B", "s0_udp3f_anonuser_3f_A", "s0_udp3f_anonuser_3f_B", "NV_s0_udp3f_anonuser_3f", "NV_s0_udp3f_anonuser_3f_A", "s0_udp3f_anonuser_3f");
    public static final ImmutableConnections s0_udp01_data = new ImmutableConnections("s0_udp01_data", ImmutableServerProfiles.SERVER_0, BaseConnections.udp01_data, 18752, "s0_udp01_anonuser_01f", "s0_udp01_anonuser_01f_B", "s0_udp01_anonuser_01f_A", "NV_s0_udp01_anonuser_01f", "NV_s0_udp01_anonuser_01f_A", "NV_s0_udp01_anonuser_01f_B");
    public static final ImmutableConnections s0_stcp01_data = new ImmutableConnections("s0_stcp01_data", ImmutableServerProfiles.SERVER_0, BaseConnections.stcp01_data, 18742, "s0_stcp01_anonuser_01f_A", "s0_stcp01_anonuser_01f_B", "s0_stcp01_anonuser_01f", "NV_s0_stcp01_anonuser_01f", "NV_s0_stcp01_anonuser_01f_B", "NV_s0_stcp01_anonuser_01f_A");
    public static final ImmutableConnections s0_mcast_data = new ImmutableConnections("s0_mcast_data", ImmutableServerProfiles.SERVER_0, BaseConnections.mcast_data, 18734, "s0_mcast_anonuser_t_A", "NV_s0_mcast_anonuser_t", "s0_mcast_anonuser_t_B", "NV_s0_mcast_anonuser_t_B", "NV_s0_mcast_anonuser_t_A", "s0_mcast_anonuser_t");
    public static final ImmutableConnections s0_stcp2f_data = new ImmutableConnections("s0_stcp2f_data", ImmutableServerProfiles.SERVER_0, BaseConnections.stcp2f_data, 18748, "NV_s0_stcp2f_anonuser_2f_B", "s0_stcp2f_anonuser_2f", "NV_s0_stcp2f_anonuser_2f_A", "s0_stcp2f_anonuser_2f_A", "s0_stcp2f_anonuser_2f_B", "NV_s0_stcp2f_anonuser_2f");
    public static final ImmutableConnections s0_stcp01t_data = new ImmutableConnections("s0_stcp01t_data", ImmutableServerProfiles.SERVER_0, BaseConnections.stcp01t_data, 18747, "NV_s0_stcp01t_anonuser_01t_A", "NV_s0_stcp01t_anonuser_01t_B", "NV_s0_stcp01t_anonuser_01t", "s0_stcp01t_anonuser_01t", "s0_stcp01t_anonuser_01t_A", "s0_stcp01t_anonuser_01t_B");
    public static final ImmutableConnections s0_tcp12_data = new ImmutableConnections("s0_tcp12_data", ImmutableServerProfiles.SERVER_0, BaseConnections.tcp12_data, 18749, "s0_tcp12_anonuser_12f_B", "s0_tcp12_anonuser_12f_A", "s0_tcp12_anonuser_12f", "NV_s0_tcp12_anonuser_12f_A", "NV_s0_tcp12_anonuser_12f_B", "NV_s0_tcp12_anonuser_12f");
    public static final ImmutableConnections s0_saproxy_data = new ImmutableConnections("s0_saproxy_data", ImmutableServerProfiles.SERVER_0, BaseConnections.saproxy_data, 18735, "s0_saproxy_anonuser_t_A", "NV_s0_saproxy_anonuser_t_B", "NV_s0_saproxy_anonuser_t_A", "s0_saproxy_anonuser_t", "s0_saproxy_anonuser_t_B", "NV_s0_saproxy_anonuser_t");
    public static final ImmutableConnections s0_authsslA_data = new ImmutableConnections("s0_authsslA_data", ImmutableServerProfiles.SERVER_0, BaseConnections.authssl_data, 18740, "s0_authsslA_authuser3_3f", "NV_s0_authsslA_authuser_f", "NV_s0_authsslA_authuser01_01f", "s0_authsslA_authuser2_2f", "NV_s0_authsslA_authuser12_012f", "s0_authsslA_authuser01_01f", "s0_authsslA_authuser12_012f", "s0_authsslA_authuser0_0f", "s0_authsslA_authuser_f", "s0_authsslA_authuser012_012f", "NV_s0_authsslA_authuser2_2f", "NV_s0_authsslA_authuser012_012f", "NV_s0_authsslA_authuser3_3f", "NV_s0_authsslA_authuser0_0f");
    public static final ImmutableConnections s0_mcast01_data = new ImmutableConnections("s0_mcast01_data", ImmutableServerProfiles.SERVER_0, BaseConnections.mcast01_data, 18755, "NV_s0_mcast01_anonuser_01f", "NV_s0_mcast01_anonuser_01f_A", "NV_s0_mcast01_anonuser_01f_B", "s0_mcast01_anonuser_01f", "s0_mcast01_anonuser_01f_A", "s0_mcast01_anonuser_01f_B");
    public static final ImmutableConnections s0_udp_data = new ImmutableConnections("s0_udp_data", ImmutableServerProfiles.SERVER_0, BaseConnections.udp_data, 18731, "s0_udp_anonuser_t", "s0_udp_anonuser_t_A", "s0_udp_anonuser_t_B", "NV_s0_udp_anonuser_t_B", "NV_s0_udp_anonuser_t", "NV_s0_udp_anonuser_t_A");
    public static final ImmutableConnections s0_ssl_data = new ImmutableConnections("s0_ssl_data", ImmutableServerProfiles.SERVER_0, BaseConnections.ssl_data, 18737, "NV_s0_ssl_anonuser_t_A", "NV_s0_ssl_anonuser_t_B", "NV_s0_ssl_anonuser_t", "s0_ssl_anonuser_t", "s0_ssl_anonuser_t_B", "s0_ssl_anonuser_t_A");
    public static final ImmutableConnections s0_authssl_data = new ImmutableConnections("s0_authssl_data", ImmutableServerProfiles.SERVER_0, BaseConnections.authssl_data, 18739, "s0_authssl_authuser12_012f", "NV_s0_authssl_authuser012_012f", "s0_authssl_authuser3_3f", "s0_authssl_authuser_f", "NV_s0_authssl_authuser3_3f", "s0_authssl_authuser012_012f", "NV_s0_authssl_authuser01_01f", "NV_s0_authssl_authuser2_2f", "s0_authssl_authuser01_01f", "NV_s0_authssl_authuser0_0f", "NV_s0_authssl_authuser_f", "s0_authssl_authuser2_2f", "NV_s0_authssl_authuser12_012f", "s0_authssl_authuser0_0f");
    public static final ImmutableConnections s0_mcast12t_data = new ImmutableConnections("s0_mcast12t_data", ImmutableServerProfiles.SERVER_0, BaseConnections.mcast12t_data, 18756, "s0_mcast12t_anonuser_12t", "NV_s0_mcast12t_anonuser_12t_B", "NV_s0_mcast12t_anonuser_12t_A", "NV_s0_mcast12t_anonuser_12t", "s0_mcast12t_anonuser_12t_A", "s0_mcast12t_anonuser_12t_B");
    public static final ImmutableConnections s0_tls_data = new ImmutableConnections("s0_tls_data", ImmutableServerProfiles.SERVER_0, BaseConnections.tls_data, 18738, "NV_s0_tls_anonuser_t_B", "NV_s0_tls_anonuser_t_A", "NV_s0_tls_anonuser_t", "s0_tls_anonuser_t_B", "s0_tls_anonuser_t_A", "s0_tls_anonuser_t");
    public static final ImmutableConnections s0_authtls_data = new ImmutableConnections("s0_authtls_data", ImmutableServerProfiles.SERVER_0, BaseConnections.authtls_data, 18741, "s0_authtls_authuser12_012f", "NV_s0_authtls_authuser01_01f", "NV_s0_authtls_authuser_f", "s0_authtls_authuser01_01f", "s0_authtls_authuser0_0f", "s0_authtls_authuser3_3f", "s0_authtls_authuser2_2f", "NV_s0_authtls_authuser12_012f", "s0_authtls_authuser012_012f", "NV_s0_authtls_authuser2_2f", "NV_s0_authtls_authuser3_3f", "NV_s0_authtls_authuser012_012f", "s0_authtls_authuser_f", "NV_s0_authtls_authuser0_0f");
    public static final ImmutableConnections s0_stcpA_data = new ImmutableConnections("s0_stcpA_data", ImmutableServerProfiles.SERVER_0, BaseConnections.stcp_data, 18733, "s0_stcpA_anonuser_t", "s0_stcpA_anonuser_t_A", "s0_stcpA_anonuser_t_B", "NV_s0_stcpA_anonuser_t", "NV_s0_stcpA_anonuser_t_B", "NV_s0_stcpA_anonuser_t_A");
    public static final ImmutableConnections s0_mcast3f_data = new ImmutableConnections("s0_mcast3f_data", ImmutableServerProfiles.SERVER_0, BaseConnections.mcast3f_data, 18757, "NV_s0_mcast3f_anonuser_3f_B", "NV_s0_mcast3f_anonuser_3f_A", "NV_s0_mcast3f_anonuser_3f", "s0_mcast3f_anonuser_3f_A", "s0_mcast3f_anonuser_3f", "s0_mcast3f_anonuser_3f_B");
    public static final ImmutableConnections s0_tcp_data = new ImmutableConnections("s0_tcp_data", ImmutableServerProfiles.SERVER_0, BaseConnections.tcp_data, 18730, "s0_tcp_anonuser_t", "s0_tcp_anonuser_t_B", "s0_tcp_anonuser_t_A", "NV_s0_tcp_anonuser_t_B", "NV_s0_tcp_anonuser_t_A", "NV_s0_tcp_anonuser_t");
    public static final ImmutableConnections s0_saproxyA_data = new ImmutableConnections("s0_saproxyA_data", ImmutableServerProfiles.SERVER_0, BaseConnections.saproxy_data, 18736, "s0_saproxyA_anonuser_t", "s0_saproxyA_anonuser_t_B", "s0_saproxyA_anonuser_t_A", "NV_s0_saproxyA_anonuser_t", "NV_s0_saproxyA_anonuser_t_B", "NV_s0_saproxyA_anonuser_t_A");
    public static final ImmutableConnections s0_stcp0_data = new ImmutableConnections("s0_stcp0_data", ImmutableServerProfiles.SERVER_0, BaseConnections.stcp0_data, 18744, "s0_stcp0_anonuser_0f", "NV_s0_stcp0_anonuser_0f_B", "NV_s0_stcp0_anonuser_0f_A", "s0_stcp0_anonuser_0f_B", "s0_stcp0_anonuser_0f_A", "NV_s0_stcp0_anonuser_0f");
    public static final ImmutableConnections s0_tcp2f_data = new ImmutableConnections("s0_tcp2f_data", ImmutableServerProfiles.SERVER_0, BaseConnections.tcp2f_data, 18751, "NV_s0_tcp2f_anonuser_2f", "NV_s0_tcp2f_anonuser_2f_A", "NV_s0_tcp2f_anonuser_2f_B", "s0_tcp2f_anonuser_2f", "s0_tcp2f_anonuser_2f_A", "s0_tcp2f_anonuser_2f_B");

    
    ////////////////////////// BD6A4745-76B4-42DD-AE35-5C2251DD6301
    // End Generated Users
    //////////////////////////


    private static CopyOnWriteArraySet<AbstractConnection> valueSet;

    private CopyOnWriteArraySet<AbstractUser> genUserSet;

    @NotNull
    private final ImmutableServerProfiles server;
    @NotNull
    private final ProtocolProfiles protocol;
    @NotNull
    private final int port;
    @NotNull
    private final AuthType authType;
    @Nullable
    private final Boolean isAnon;
    @NotNull
    private final GroupSetProfiles groupSet;
    @NotNull
    private final String consistentUniqueReadableIdentifier;
    @NotNull
    private final BaseConnections connectionModel;
    @Nullable
    private final String mcastGroup;
    @Nullable
    private final String type;
    private String[] genUsersStrings;

    public BaseConnections getConnectionModel() {
        return connectionModel;
    }

    private ImmutableConnections(@NotNull String identifier, @NotNull ImmutableServerProfiles source, @NotNull BaseConnections modelInput, @NotNull int port, @NotNull String... genUserNameList) {
        this.consistentUniqueReadableIdentifier = identifier;
        this.server = source;
        this.protocol = modelInput.getProtocol();
        this.port = port;
        this.authType = modelInput.getAuthType();
        this.groupSet = modelInput.getGroupSet();
        this.isAnon = modelInput.getRawAnon();
        this.mcastGroup = modelInput.getMcastGroup();
        this.type = modelInput.getType();
        this.genUsersStrings = genUserNameList;
        this.connectionModel = modelInput;

        valueMap.put(identifier, this);
    }

    public static CopyOnWriteArraySet<AbstractConnection> valueSet() {
        return valueSet;
    }

    public static CopyOnWriteArraySet<AbstractConnection> valuesFiltered(@NotNull ConnectionFilter filter) {
        initStaticValuesIfNecessary();

        return filter.filterConnections(valueSet);
    }

    public static ImmutableConnections valueOf(@NotNull String key) {
        return valueMap.get(key);
    }

    public static ImmutableConnections[] values() {
        return (ImmutableConnections[]) valueMap.values().toArray();
    }


    private static synchronized void initStaticValuesIfNecessary() {
        if (!valueMap.isEmpty()) {
            if (valueSet == null) {
                valueSet = new CopyOnWriteArraySet<AbstractConnection>(valueMap.values());
            }
        }
    }

    private synchronized void initValuesIfNecessary() {
        if (genUsersStrings != null) {
            if (genUserSet == null) {
                Set<AbstractUser> userSet = new HashSet<>();

                for (int i = 0; i < genUsersStrings.length; i++) {
                    String username = genUsersStrings[i];
                    ImmutableUsers user = ImmutableUsers.valueOf(username);
                    userSet.add(user);
                }
                genUserSet = new CopyOnWriteArraySet<>(userSet);
            }
            genUsersStrings = null;
        }
    }

    public synchronized CopyOnWriteArraySet<AbstractUser> getUsers(@Nullable UserFilter filter) {
        initValuesIfNecessary();

        if (filter == null) {
            return genUserSet;
        } else {
            return filter.filterUsers(genUserSet);
        }
    }

    @Override
    public Boolean getRawAnonAccessFlag() {
        return isAnon;
    }

    @Override
    public ProtocolProfiles getProtocol() {
        return protocol;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public AuthType getAuthType() {
        return authType;
    }

    @Override
    public GroupSetProfiles getGroupSet() {
        return groupSet;
    }

    @Override
    public String getMCastGroup() {
        return mcastGroup;
    }

    @Override
    public String getConsistentUniqueReadableIdentifier() {
        return consistentUniqueReadableIdentifier;
    }

    @Override
    public String getDynamicName() {
        return consistentUniqueReadableIdentifier;
    }

	@Override
	public String getType() {
		return type;
	}

    @Override
    public AbstractServerProfile getServer() {
        return server;
    }
}
