/// Error types for BMS protocol parsing
#[derive(Debug, thiserror::Error)]
#[cfg_attr(feature = "android", derive(uniffi::Error))]
pub enum Error {
    #[error("Not enough data")]
    NotEnoughData,
    #[error("Bad record type")]
    BadRecordType,
    #[error("Bad CRC checksum")]
    BadCrc,
    #[error("Invalid data: {msg}")]
    InvalidData { msg: String },
}

pub type Result<T> = core::result::Result<T, Error>;
