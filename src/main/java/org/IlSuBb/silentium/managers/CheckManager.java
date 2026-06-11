package org.IlSuBb.silentium.managers;

import org.IlSuBb.silentium.Silentium;
import org.IlSuBb.silentium.checks.CheckBase;
import org.IlSuBb.silentium.checks.blatant.*;
import org.IlSuBb.silentium.checks.ghost.*;
import org.IlSuBb.silentium.checks.anarchy.*;

import java.util.*;

public class CheckManager {

    private final List<CheckBase> checks = new ArrayList<>();
    private final Map<String, CheckBase> byName = new LinkedHashMap<>();

    public CheckManager(Silentium plugin) {
        // Blatant
        register(new FlyCheck(plugin));
        register(new SpeedCheck(plugin));
        register(new KillAuraCheck(plugin));
        register(new ReachCheck(plugin));
        register(new NoFallCheck(plugin));
        register(new AutoClickerCheck(plugin));
        register(new AntiKnockbackCheck(plugin));
        register(new ScaffoldCheck(plugin));
        // Ghost
        register(new GhostReachCheck(plugin));
        register(new AimAssistCheck(plugin));
        register(new VelocityCheck(plugin));
        register(new TimerCheck(plugin));
        register(new GhostAutoClickerCheck(plugin));
        register(new GhostTargetCheck(plugin));
        // Anarchy
        register(new XRayCheck(plugin));
        register(new FastPlaceCheck(plugin));
        register(new PhaseCheck(plugin));
    }

    private void register(CheckBase check) {
        checks.add(check);
        byName.put(check.getName().toLowerCase(), check);
    }

    public List<CheckBase> getChecks() { return Collections.unmodifiableList(checks); }

    public Optional<CheckBase> getCheck(String name) {
        return Optional.ofNullable(byName.get(name.toLowerCase()));
    }

    @SuppressWarnings("unchecked")
    public <T extends CheckBase> T get(Class<T> type) {
        for (CheckBase c : checks) {
            if (type.isInstance(c)) return (T) c;
        }
        return null;
    }

    public long getEnabledCount() {
        return checks.stream().filter(CheckBase::isEnabled).count();
    }
}
