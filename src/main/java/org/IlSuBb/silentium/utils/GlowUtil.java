package org.IlSuBb.silentium.utils;

import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Sends a per-player entity glow (ClientboundSetEntityDataPacket) to a single client
 * without touching the server-side entity state visible to everyone.
 *
 * Uses reflection so no NMS compile dependency is needed.
 */
public final class GlowUtil {

    private GlowUtil() {}

    // ── Reflection cache (initialised once) ────────────────────────────────

    private static volatile boolean ready;   // true if init succeeded
    private static volatile boolean tried;   // true if init was attempted

    private static Method   mGetHandle;       // CraftEntity.getHandle()
    private static Field    fDataFlagsId;     // Entity.DATA_SHARED_FLAGS_ID
    private static Method   mGetEntityData;   // Entity.getEntityData()
    private static Method   mSynchedGet;      // SynchedEntityData.get(accessor)
    private static Method   mDataValueCreate; // SynchedEntityData.DataValue.create(acc, val)
    private static Constructor<?> cPacket;    // ClientboundSetEntityDataPacket(int, List)
    private static Field    fConnection;      // ServerPlayer.connection
    private static Method   mConnSend;        // ServerCommonPacketListenerImpl.send(Packet)

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Send glow=true / glow=false entity metadata only to {@code receiver}.
     * Call is silently ignored if NMS reflection initialisation failed.
     */
    public static void sendGlow(Player receiver, Player target, boolean glowing) {
        if (!ensureInit(receiver)) return;
        try {
            Object nmsTarget   = mGetHandle.invoke(target);
            Object nmsReceiver = mGetHandle.invoke(receiver);

            Object entityData = mGetEntityData.invoke(nmsTarget);
            Object accessor   = fDataFlagsId.get(null);
            byte   cur        = (Byte) mSynchedGet.invoke(entityData, accessor);
            byte   updated    = glowing
                    ? (byte) (cur |  0x40)   // set   glow bit (bit 6)
                    : (byte) (cur & ~0x40);  // clear glow bit

            Object dv     = mDataValueCreate.invoke(null, accessor, updated);
            Object packet = cPacket.newInstance(target.getEntityId(), List.of(dv));

            Object conn = fConnection.get(nmsReceiver);
            mConnSend.invoke(conn, packet);
        } catch (Exception ignored) {}
    }

    public static boolean isReady() { return ready; }

    // ── Initialisation ─────────────────────────────────────────────────────

    private static synchronized boolean ensureInit(Player sample) {
        if (tried) return ready;
        tried = true;
        try {
            // 1. CraftPlayer.getHandle() → ServerPlayer
            mGetHandle = sample.getClass().getMethod("getHandle");

            Object nmsPlayer = mGetHandle.invoke(sample);
            Class<?> nmsClass = nmsPlayer.getClass();

            // 2. DATA_SHARED_FLAGS_ID — static field, walk up class hierarchy
            fDataFlagsId = findStaticField(nmsClass, "DATA_SHARED_FLAGS_ID");
            if (fDataFlagsId == null) return false;
            Object accessor = fDataFlagsId.get(null);

            // 3. Entity.getEntityData() → SynchedEntityData
            mGetEntityData = nmsClass.getMethod("getEntityData");
            Object synchedData = mGetEntityData.invoke(nmsPlayer);

            // 4. SynchedEntityData.get(EntityDataAccessor<T>) — pick overload that accepts accessor
            for (Method m : synchedData.getClass().getMethods()) {
                if (m.getName().equals("get") && m.getParameterCount() == 1) {
                    try { m.invoke(synchedData, accessor); mSynchedGet = m; break; }
                    catch (Exception ignored) {}
                }
            }
            if (mSynchedGet == null) return false;

            // 5. SynchedEntityData.DataValue.create(accessor, value) — static
            Class<?> dvClass = Class.forName("net.minecraft.network.syncher.SynchedEntityData$DataValue");
            for (Method m : dvClass.getDeclaredMethods()) {
                if (m.getName().equals("create") && m.getParameterCount() == 2
                        && Modifier.isStatic(m.getModifiers())) {
                    m.setAccessible(true);
                    mDataValueCreate = m;
                    break;
                }
            }
            if (mDataValueCreate == null) return false;

            // 6. ClientboundSetEntityDataPacket(int id, List<DataValue<?>>) constructor
            Class<?> pktClass = Class.forName(
                    "net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket");
            for (Constructor<?> c : pktClass.getDeclaredConstructors()) {
                if (c.getParameterCount() == 2) { c.setAccessible(true); cPacket = c; break; }
            }
            if (cPacket == null) return false;

            // 7. ServerPlayer.connection field
            fConnection = findField(nmsClass, "connection");
            if (fConnection == null) return false;
            Object conn = fConnection.get(nmsPlayer);

            // 8. send(Packet<?>) on the connection object
            for (Method m : conn.getClass().getMethods()) {
                if (m.getName().equals("send") && m.getParameterCount() == 1) {
                    mConnSend = m; break;
                }
            }
            if (mConnSend == null) return false;

            ready = true;
        } catch (Exception ignored) {
            ready = false;
        }
        return ready;
    }

    // ── Reflection helpers ─────────────────────────────────────────────────

    private static Field findStaticField(Class<?> clazz, String name) {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                if (Modifier.isStatic(f.getModifiers())) { f.setAccessible(true); return f; }
            } catch (NoSuchFieldException ignored) {}
        }
        return null;
    }

    private static Field findField(Class<?> clazz, String name) {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f; }
            catch (NoSuchFieldException ignored) {}
        }
        return null;
    }
}
