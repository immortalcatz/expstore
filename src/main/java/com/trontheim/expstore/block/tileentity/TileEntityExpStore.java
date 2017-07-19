package com.trontheim.expstore.block.tileentity;

import com.google.gson.Gson;
import com.trontheim.expstore.ExperienceStore;
import com.trontheim.expstore.init.ESBlocks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;

import java.util.HashMap;
import java.util.UUID;

public class TileEntityExpStore extends TileEntity {

  private ExperienceStorage experienceStorage = new ExperienceStorage();

  public boolean storeExperiencePoints(EntityPlayer player) {
    if(0 == player.experienceTotal) {
      return false;
    }

    Integer amount = player.experienceTotal;

    if(!experienceStorage.addExperiencePoints(player, amount)) {
      return false;
    }

    player.addExperienceLevel(-(player.experienceLevel + 1));

    player.addChatComponentMessage(new ChatComponentText("experience points stored: " + amount.toString()));

    return true;
  }

  public boolean restoreExperiencePoints(EntityPlayer player) {
    if(0 == experienceStorage.getExperiencePoints(player)) {
      return false;
    }

    Integer amount = experienceStorage.getExperiencePoints(player);

    if(!experienceStorage.addExperiencePoints(player, -amount)) {
      return false;
    }

    player.addExperience(amount);

    player.addChatComponentMessage(new ChatComponentText("experience points restored: " + amount.toString()));

    return true;
  }

  public boolean resetStore(EntityPlayer player) {

    experienceStorage.reset();

    player.addChatComponentMessage(new ChatComponentText("all stored experience points deleted."));

    return true;
  }

  public Integer getSoredExperiencePoints(EntityPlayer player) {
    return experienceStorage.getExperiencePoints(player);
  }

  @Override
  public Packet getDescriptionPacket() {
    NBTTagCompound nbttagcompound = new NBTTagCompound();
    writeToNBT(nbttagcompound);
    return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 0, nbttagcompound);
  }

  @Override
  public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
    NBTTagCompound tag = pkt.func_148857_g();
    readFromNBT(tag);
  }

  @Override
  public void readFromNBT(NBTTagCompound compound) {
    super.readFromNBT(compound);

    NBTTagCompound data = compound.getCompoundTag(ExperienceStore.MODID + ":storage");

    // convert old experience json string to nbt tag compound
    if(compound.hasKey(ExperienceStore.MODID + ":TileEntityExpStore")) {
      ExperienceStorage oldData = new Gson().fromJson(compound.getString(ExperienceStore.MODID + ":TileEntityExpStore"), ExperienceStorage.class);
      data = oldData.exportData();
    }

    experienceStorage.importData(data);
  }

  @Override
  public void writeToNBT(NBTTagCompound compound) {
    super.writeToNBT(compound);
    compound.setTag(ExperienceStore.MODID + ":storage", experienceStorage.exportData());

    // remove old experience json string
    if(compound.hasKey(ExperienceStore.MODID + ":TileEntityExpStore")) {
      compound.removeTag(ExperienceStore.MODID + ":TileEntityExpStore");
    }
  }

  private static class ExperienceStorage {

    private HashMap<String, Integer> experience = new HashMap<String, Integer>();

    private boolean reset() {
      experience = new HashMap<String, Integer>();

      return true;
    }

    private String getUUID(EntityPlayer player) {
      return ESBlocks.isDevelopmentEnvironment() ? "DEVPLAYER" : player.getUniqueID().toString();
    }

    private boolean addExperiencePoints(EntityPlayer player, Integer amount) {
      String uuid = getUUID(player);

      if(!experience.containsKey(uuid)) {
        experience.put(uuid, 0);
      }

      // get current stored experience
      Integer currentAmount = experience.get(uuid);

      // check max integer value to avoid exceptions
      int maxAmount = Integer.MAX_VALUE - currentAmount;
      if(amount > maxAmount) {
        amount = maxAmount;
      }

      // check min integer value to avoid exceptions
      int minAmount = Integer.MIN_VALUE + currentAmount;
      if(amount < minAmount) {
        amount = minAmount;
      }

      // change experience
      currentAmount += amount;

      // check if stored experience lesser than zero and set it to null
      if(currentAmount < 0) {
        currentAmount = 0;
      }

      experience.put(uuid, currentAmount);

      return true;
    }

    private Integer getExperiencePoints(EntityPlayer player) {
      String uuid = getUUID(player);

      return experience.containsKey(uuid) ? experience.get(uuid) : 0;
    }

    private NBTTagCompound exportData() {
      NBTTagCompound data = new NBTTagCompound();
      for(HashMap.Entry<String, Integer> entry: experience.entrySet()) {
        data.setInteger(entry.getKey(), entry.getValue());
      }

      return data;
    }

    private void importData(NBTTagCompound data) {
      reset();
      for(Object key: data.func_150296_c()) {
        experience.put(key.toString(), data.getInteger(key.toString()));
      }
    }

  }

}
