package dev.swathe;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: "this is the shape I want my swings to cut".
 *
 * <p>Registering this type is also what lets the client detect server support -
 * {@code ClientPlayNetworking.canSend(TYPE)} is only true when the server declared it
 * can receive this channel, which means the mod is installed over there.
 */
public record SwathePayload(ShapeSpec spec) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SwathePayload> TYPE =
			new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Swathe.MOD_ID, "shape"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SwathePayload> CODEC = StreamCodec.of(
			(buf, payload) -> {
				ShapeSpec spec = payload.spec();
				buf.writeVarInt(spec.left());
				buf.writeVarInt(spec.right());
				buf.writeVarInt(spec.up());
				buf.writeVarInt(spec.down());
				buf.writeVarInt(spec.depth());
				buf.writeBoolean(spec.requireCorrectTool());
			},
			buf -> new SwathePayload(new ShapeSpec(
					buf.readVarInt(),
					buf.readVarInt(),
					buf.readVarInt(),
					buf.readVarInt(),
					buf.readVarInt(),
					buf.readBoolean())));

	/** Must run on both sides: the client needs it to send, the server to receive. */
	public static void register() {
		PayloadTypeRegistry.playC2S().register(TYPE, CODEC);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
