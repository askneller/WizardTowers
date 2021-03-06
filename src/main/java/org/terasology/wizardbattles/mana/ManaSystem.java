// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.wizardbattles.mana;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.characters.AliveCharacterComponent;
import org.terasology.logic.delay.DelayManager;
import org.terasology.logic.delay.PeriodicActionTriggeredEvent;
import org.terasology.logic.players.event.OnPlayerSpawnedEvent;
import org.terasology.registry.In;
import org.terasology.world.WorldComponent;

@RegisterSystem
public class ManaSystem extends BaseComponentSystem {

    private static final Logger logger = LoggerFactory.getLogger(ManaSystem.class);
    private static final String MANA_REGEN_ACTION_ID = "ManaRegen";
    private static final long MANA_REGEN_INTERVAL = 3000L;

    @In
    private EntityManager entityManager;

    @In
    private DelayManager delayManager;

    public void postBegin() {
        boolean processedOnce = false;
        for (EntityRef entity : entityManager.getEntitiesWith(WorldComponent.class)) {
            if (!processedOnce) {
                delayManager.addPeriodicAction(entity, MANA_REGEN_ACTION_ID, MANA_REGEN_INTERVAL, MANA_REGEN_INTERVAL);
                processedOnce = true;
            } else {
                logger.warn("More than one entity with WorldComponent found");
            }
        }
    }

    @ReceiveEvent(components = ManaComponent.class)
    public void onPlayerSpawn(OnPlayerSpawnedEvent event, EntityRef player) {
        ManaComponent manaComponent = player.getComponent(ManaComponent.class);
        manaComponent.current = manaComponent.maximum;
        logger.info("Initialising player mana to {}", manaComponent.current);
        player.saveComponent(manaComponent);
    }

    @ReceiveEvent
    public void onPeriodicActionTriggered(PeriodicActionTriggeredEvent event, EntityRef unusedEntity) {
        if (event.getActionId().equals(MANA_REGEN_ACTION_ID)) {
            for (EntityRef entity : entityManager.getEntitiesWith(ManaComponent.class, AliveCharacterComponent.class)) {
                ManaComponent manaComponent = entity.getComponent(ManaComponent.class);
                if (manaComponent.current < manaComponent.maximum) {
                    int regenRate = manaComponent.regenRate;
                    int amount = Math.min(regenRate, manaComponent.maximum - manaComponent.current);
                    if (amount < 0) {
                        amount = 0;
                    }
                    if (amount > 0) {
                        manaComponent.current += amount;
                        entity.saveComponent(manaComponent);
                    }
                }
            }
        }
    }

    @ReceiveEvent(components = ManaComponent.class)
    public void onConsumeMana(ConsumeManaEvent event, EntityRef player) {
        ManaComponent manaComponent = player.getComponent(ManaComponent.class);
        manaComponent.current -= event.getAmount();
        player.saveComponent(manaComponent);
    }
}
