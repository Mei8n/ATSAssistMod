package jp.kaiz.atsassistmod.block.tileentity;

import jp.kaiz.atsassistmod.ATSAssistBlock;
import jp.kaiz.atsassistmod.block.GroundUnitType;
import jp.kaiz.atsassistmod.controller.SpeedOrder;
import jp.kaiz.atsassistmod.controller.TrainControllerManager;
import jp.kaiz.atsassistmod.controller.trainprotection.TrainProtectionType;
import jp.ngt.rtm.entity.train.EntityTrainBase;
import jp.ngt.rtm.entity.train.util.TrainState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

import java.util.List;

public abstract class TileEntityGroundUnit extends TileEntity {
    //編成単位での管理
    protected long formationID;
    //レッドストーン連動
    protected boolean linkRedStone;
    //外に出すレッドストーン
    private int redStoneOutput;

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.formationID = tag.getLong("formationID");
        this.linkRedStone = tag.getBoolean("linkRedStone");
        this.redStoneOutput = tag.getInteger("redStoneOutput");
        this.readNBT(tag);
    }

    protected abstract void readNBT(NBTTagCompound tag);

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setLong("formationID", this.formationID);
        tag.setBoolean("linkRedStone", this.linkRedStone);
        tag.setInteger("redStoneOutput", this.redStoneOutput);
        this.writeNBT(tag);
    }

    protected abstract void writeNBT(NBTTagCompound tag);

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound nbtTagCompound = new NBTTagCompound();
        this.writeToNBT(nbtTagCompound);
        return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 1, nbtTagCompound);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        this.readFromNBT(pkt.func_148857_g());
    }

    @Override
    public void updateEntity() {
        if (!this.worldObj.isRemote) {
            if (!this.linkRedStone || this.worldObj.isBlockIndirectlyGettingPowered(this.xCoord, this.yCoord, this.zCoord)) {//レッドストーン確認
                AxisAlignedBB detect = AxisAlignedBB.getBoundingBox(
                        this.xCoord, this.yCoord, this.zCoord, this.xCoord + 1, this.yCoord + 3, this.zCoord + 1);
                List<?> list = this.worldObj.getEntitiesWithinAABB(EntityTrainBase.class, detect);
                if (!list.isEmpty()) {
                    EntityTrainBase train = (EntityTrainBase) list.get(0);
                    if (train.isControlCar()) {
                        if (this.formationID != train.getFormation().id) {
                            this.onTick(train);
                            this.formationID = train.getFormation().id;
                        }
                        return;
                    }
                }
            }
            this.formationID = 0;
        }
    }

    public int getRedStoneOutput() {
        return this.redStoneOutput;
    }

    public void setRedStoneOutput(int power) {
        if (this.redStoneOutput != power) {
            this.redStoneOutput = power;
            this.worldObj.notifyBlockChange(this.xCoord, this.yCoord, this.zCoord, this.getBlockType());
        }
    }

    protected abstract void onTick(EntityTrainBase train);

    public abstract GroundUnitType getType();

    public void setLinkRedStone(boolean linkRedStone) {
        this.linkRedStone = linkRedStone;
    }

    public boolean isLinkRedStone() {
        return linkRedStone;
    }

    //パケット用共通パーツ

    public interface Speed {
        void setSpeedLimit(int speedLimit);

        int getSpeedLimit();
    }

    public interface Distance {
        void setDistance(double distance);

        double getDistance();
    }

    public static class None extends TileEntityGroundUnit {

        @Override
        public void readNBT(NBTTagCompound tag) {

        }

        @Override
        public void writeNBT(NBTTagCompound tag) {

        }

        @Override
        public void onTick(EntityTrainBase train) {

        }

        @Override
        public GroundUnitType getType() {
            return GroundUnitType.None;
        }
    }

    public static class ATCSpeedLimitNotice extends TileEntityGroundUnit implements Speed, Distance {
        private int speedLimit;
        private double distance;

        @Override
        public void onTick(EntityTrainBase train) {
            SpeedOrder speedOrder = new SpeedOrder(this.speedLimit, this.distance);
            TrainControllerManager.getTrainController(train).addSpeedOrder(speedOrder);
        }

        @Override
        public void readNBT(NBTTagCompound tag) {
            this.speedLimit = tag.getInteger("speedLimit");
            this.distance = tag.getDouble("distance");
        }

        @Override
        public void writeNBT(NBTTagCompound tag) {
            tag.setInteger("speedLimit", this.speedLimit);
            tag.setDouble("distance", this.distance);
        }

        @Override
        public GroundUnitType getType() {
            return GroundUnitType.ATC_SpeedLimit_Notice;
        }

        @Override
        public void setSpeedLimit(int speedLimit) {
            this.speedLimit = speedLimit;
        }

        @Override
        public int getSpeedLimit() {
            return this.speedLimit;
        }

        @Override
        public void setDistance(double distance) {
            this.distance = distance;
        }

        @Override
        public double getDistance() {
            return this.distance;
        }
    }

    public static class ATCSpeedLimitCancel extends TileEntityGroundUnit {
        //編成最後尾で解除
        private boolean lateCancel;

        @Override
        public void readNBT(NBTTagCompound tag) {
            this.lateCancel = tag.getBoolean("lateCancel");
        }

        @Override
        public void writeNBT(NBTTagCompound tag) {
            tag.setBoolean("lateCancel", lateCancel);
        }

        @Override
        public void onTick(EntityTrainBase train) {
            if (this.formationID != train.getFormation().id) {
                TrainControllerManager.getTrainController(train).removeSpeedLimit();
                this.formationID = train.getFormation().id;
            }
        }

        @Override
        public void updateEntity() {
            if (!this.worldObj.isRemote) {
                if (!this.linkRedStone || this.worldObj.isBlockIndirectlyGettingPowered(this.xCoord, this.yCoord, this.zCoord)) {//レッドストーン確認
                    AxisAlignedBB detect = AxisAlignedBB.getBoundingBox(
                            this.xCoord, this.yCoord, this.zCoord, this.xCoord + 1, this.yCoord + 3, this.zCoord + 1);
                    List<?> list = this.worldObj.getEntitiesWithinAABB(EntityTrainBase.class, detect);
                    if (!list.isEmpty()) {
                        EntityTrainBase train = (EntityTrainBase) list.get(0);
                        if (this.lateCancel) {
                            if (train.getFormation().size() == 1) {
                                this.onTick(train);
                                return;
                            } else if (!train.isControlCar() && (train.getConnectedTrain(0) == null || train.getConnectedTrain(1) == null)) {
                                this.onTick(train);
                                return;
                            }
                        } else {
                            if (train.isControlCar()) {
                                this.onTick(train);
                                return;
                            }
                        }
                    }
                }
                this.formationID = 0;
            }
        }

        @Override
        public GroundUnitType getType() {
            return GroundUnitType.ATC_SpeedLimit_Cancel;
        }

        public void setLateCancel(boolean lateCancel) {
            this.lateCancel = lateCancel;
        }

        public boolean isLateCancel() {
            return this.lateCancel;
        }
    }

    public static class TASCStopPositionNotice extends TileEntityGroundUnit implements Distance {
        private double distance;
        private byte version;

        public TASCStopPositionNotice() {
            version = 1;
        }

        @Override
        public void readNBT(NBTTagCompound tag) {
            this.distance = tag.getDouble("distance");
            this.version = tag.getByte("version");
        }

        @Override
        public void writeNBT(NBTTagCompound tag) {
            tag.setDouble("distance", this.distance);
            tag.setByte("version", this.version);
        }

        @Override
        public void onTick(EntityTrainBase train) {
            TrainControllerManager.getTrainController(train).tascController.enable(this.distance + 1.5d);
        }

        @Override
        public void updateEntity() {
            super.updateEntity();
            if (!worldObj.isRemote) {
                if (this.version == 0) {
                    this.distance = this.distance - 2;
                    this.version = 1;
                    this.markDirty();
                    this.getDescriptionPacket();
                    this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
                    this.worldObj.notifyBlockChange(this.xCoord, this.yCoord, this.zCoord, ATSAssistBlock.blockGroundUnit);
                }
            }
        }

        @Override
        public GroundUnitType getType() {
            return GroundUnitType.TASC_StopPotion_Notice;
        }

        @Override
        public void setDistance(double distance) {
            this.distance = distance;
        }

        @Override
        public double getDistance() {
            return this.distance;
        }
    }

    public static class TASCDisable extends TileEntityGroundUnit {
        @Override
        public void readNBT(NBTTagCompound tag) {

        }

        @Override
        public void writeNBT(NBTTagCompound tag) {

        }

        @Override
        public void onTick(EntityTrainBase train) {
            TrainControllerManager.getTrainController(train).tascController.disable();
        }

        @Override
        public GroundUnitType getType() {
            return GroundUnitType.TASC_Cancel;
        }
    }

    public static class TASCStopPositionCorrection extends TileEntityGroundUnit implements Distance {
        private double distance;
        private byte version;

        public TASCStopPositionCorrection() {
            version = 1;
        }

        @Override
        public void readNBT(NBTTagCompound tag) {
            this.distance = tag.getDouble("distance");
            this.version = tag.getByte("version");
        }

        @Override
        public void writeNBT(NBTTagCompound tag) {
            tag.setDouble("distance", this.distance);
            tag.setByte("version", this.version);
        }

        @Override
        public void onTick(EntityTrainBase train) {
            TrainControllerManager.getTrainController(train).tascController.setStopDistance(this.distance + 1.5d);
        }

        @Override
        public void updateEntity() {
            super.updateEntity();
            if (!worldObj.isRemote) {
                if (this.version == 0) {
                    this.distance = this.distance - 2;
                    this.version = 1;
                    this.markDirty();
                    this.getDescriptionPacket();
                    this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
                    this.worldObj.notifyBlockChange(this.xCoord, this.yCoord, this.zCoord, ATSAssistBlock.blockGroundUnit);
                }
            }
        }

        @Override
        public GroundUnitType getType() {
            return GroundUnitType.TASC_StopPotion_Correction;
        }

        @Override
        public void setDistance(double distance) {
            this.distance = distance;
        }

        @Override
        public double getDistance() {
            return this.distance;
        }
    }

    public static class TASCStopPosition extends TileEntityGroundUnit {

        @Override
        public void readNBT(NBTTagCompound tag) {
        }

        @Override
        public void writeNBT(NBTTagCompound tag) {
        }

        @Override
        public void updateEntity() {
            if (!this.worldObj.isRemote) {
                AxisAlignedBB detect = AxisAlignedBB.getBoundingBox(
                        this.xCoord, this.yCoord, this.zCoord, this.xCoord + 1, this.yCoord + 3, this.zCoord + 1);
                List<?> list = this.worldObj.getEntitiesWithinAABB(EntityTrainBase.class, detect);
                if (!list.isEmpty()) {
                    EntityTrainBase train = (EntityTrainBase) list.get(0);
                    if (this.linkRedStone) {//逆転前以外でも
                        this.onTick(train);
                        return;
                    } else {
                        if (train.isControlCar()) {
                            this.onTick(train);
                            return;
                        }
                    }
                }
                this.setRedStoneOutput(0);
            }
        }

        @Override
        public void onTick(EntityTrainBase train) {
            this.setRedStoneOutput(train.getSpeed() == 0F ? train.getFormation().size() : 0);
        }

        @Override
        public GroundUnitType getType() {
            return GroundUnitType.TASC_StopPotion;
        }
    }

    public static class ATODepartureSignal extends TileEntityGroundUnit implements Speed {
        private int speedLimit;

        @Override
        public void readNBT(NBTTagCompound tag) {
            this.speedLimit = tag.getInteger("speedLimit");
        }

        @Override
        public void writeNBT(NBTTagCompound tag) {
            tag.setInteger("speedLimit", this.speedLimit);
        }

        @Override
        public void onTick(EntityTrainBase train) {
            TrainControllerManager.getTrainController(train).enableATO(this.speedLimit);
        }

        @Override
        public GroundUnitType getType() {
            return GroundUnitType.ATO_Departure_Signal;
        }


        @Override
        public void setSpeedLimit(int speedLimit) {
            this.speedLimit = speedLimit;
        }

        @Override
        public int getSpeedLimit() {
            return this.speedLimit;
        }
    }

    public static class ATODisable extends TileEntityGroundUnit {
        @Override
        public void readNBT(NBTTagCompound tag) {

        }

        @Override
        public void writeNBT(NBTTagCompound tag) {

        }

        @Override
        public void onTick(EntityTrainBase train) {
            TrainControllerManager.getTrainController(train).disableATO();
        }

        @Override
        public GroundUnitType getType() {
            return GroundUnitType.ATO_Cancel;
        }
    }

    public static class ATOChangeSpeed extends TileEntityGroundUnit implements Speed {
        private int speedLimit;

        @Override
        public void readNBT(NBTTagCompound tag) {
            this.speedLimit = tag.getInteger("speedLimit");
        }

        @Override
        public void writeNBT(NBTTagCompound tag) {
            tag.setInteger("speedLimit", this.speedLimit);
        }

        @Override
        public void onTick(EntityTrainBase train) {
            TrainControllerManager.getTrainController(train).setMaxSpeed(this.speedLimit);
        }

        @Override
        public GroundUnitType getType() {
            return GroundUnitType.ATO_Change_Speed;
        }


        @Override
        public void setSpeedLimit(int speedLimit) {
            this.speedLimit = speedLimit;
        }

        @Override
        public int getSpeedLimit() {
            return this.speedLimit;
        }
    }

    public static class TrainStateSet extends TileEntityGroundUnit {
        private byte[] states;

        public TrainStateSet() {
            states = new byte[]{
                    -1,
                    -9,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,};
        }

        @Override
        public void readNBT(NBTTagCompound tag) {
            this.states = tag.getByteArray("state");
        }

        @Override
        public void writeNBT(NBTTagCompound tag) {
            tag.setByteArray("state", states);
        }

        @Override
        public void onTick(EntityTrainBase train) {
            for (int i = 0; i < 12; i++) {
                if (i == 3) {
                    continue;
                }
                if (states[i] < TrainState.getStateType(i).min) {
                    continue;
                }
                if (i == TrainState.TrainStateType.State_TrainDir.id) {
                    train.setTrainDirection(states[i]);
                    continue;
                }
                train.setTrainStateData(i, states[i]);
            }

        }

        @Override
        public GroundUnitType getType() {
            return GroundUnitType.TrainState_Set;
        }

        public byte[] getStates() {
            return this.states;
        }

        public void setStates(byte[] states) {
            this.states = states;
        }

        @Override
        public void updateEntity() {
            if (!this.worldObj.isRemote) {
                if (this.worldObj.isBlockIndirectlyGettingPowered(this.xCoord, this.yCoord, this.zCoord)) {//レッドストーン確認
                    AxisAlignedBB detect = AxisAlignedBB.getBoundingBox(
                            this.xCoord, this.yCoord, this.zCoord, this.xCoord + 1, this.yCoord + 3, this.zCoord + 1);
                    List<?> list = this.worldObj.getEntitiesWithinAABB(EntityTrainBase.class, detect);
                    if (!list.isEmpty()) {
                        EntityTrainBase train = (EntityTrainBase) list.get(0);
                        if (this.linkRedStone) {//逆転前以外でも
                            this.onTick(train);
                        } else {
                            if (train.isControlCar()) {
                                this.onTick(train);
                            }
                        }
                    }
                }
            }
        }
    }

    public static class ChangeTrainProtection extends TileEntityGroundUnit {
        private byte version;
        private int tpType;

        public ChangeTrainProtection() {
            this(TrainProtectionType.NONE);
        }


        public ChangeTrainProtection(TrainProtectionType type) {
            this.version = 1;
            this.tpType = type.id;
        }


        @Override
        protected void readNBT(NBTTagCompound tag) {
            this.version = tag.getByte("version");
            this.tpType = tag.getInteger("tpType");
        }

        @Override
        public void writeNBT(NBTTagCompound tag) {
            tag.setByte("version", this.version);
            tag.setInteger("tpType", this.tpType);
        }

        @Override
        public void updateEntity() {
            if (!worldObj.isRemote) {
                switch (this.version) {
                    case 0:
                        this.tpType = TrainProtectionType.ATACS.id;
                        this.version = 1;
                        break;
                    case 1:
                        super.updateEntity();
                        break;
                }
            }
        }

        @Override
        protected void onTick(EntityTrainBase train) {
            TrainControllerManager.getTrainController(train).setTrainProtection(TrainProtectionType.getType(this.tpType));
//            TrainControllerManager.getTrainController(train).enableTrainProtection(TrainProtectionType.ATACS);
        }

        @Override
        public GroundUnitType getType() {
            return GroundUnitType.CHANGE_TP;
        }

        public void setTPType(TrainProtectionType type) {
            this.tpType = type.id;
        }

        public TrainProtectionType getTPType() {
            return TrainProtectionType.getType(this.tpType);
        }
    }

    public static class ATACSDisable extends TileEntityGroundUnit {

        @Override
        protected void readNBT(NBTTagCompound tag) {
        }

        @Override
        public void writeNBT(NBTTagCompound tag) {
        }

        @Override
        protected void onTick(EntityTrainBase train) {
//            TrainControllerManager.getTrainController(train).disableTrainProtection();
        }

        @Override
        public void updateEntity() {
            if (!worldObj.isRemote) {
                this.worldObj.setBlock(this.xCoord, this.yCoord, this.zCoord, ATSAssistBlock.blockGroundUnit, 14, 3);
                ((ChangeTrainProtection) this.worldObj.getTileEntity(this.xCoord, this.yCoord, this.zCoord)).setTPType(TrainProtectionType.NONE);
                this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
                this.worldObj.notifyBlockChange(this.xCoord, this.yCoord, this.zCoord, ATSAssistBlock.blockGroundUnit);
            }
        }

        @Override
        public GroundUnitType getType() {
            return GroundUnitType.ATACS_Disable;
        }
    }
}
