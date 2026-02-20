mod error;
mod protocol;
mod types;
mod utils;

pub use error::{Error, Result};
pub use protocol::{
    CHARACTERISTIC_UUID, CMD_CELL_DATA, CMD_DEVICE_INFO, RECORD_TYPE_CELL_DATA,
    RECORD_TYPE_DEVICE_INFO, REQUEST_HEADER, RESPONSE_HEADER, SERVICE_UUID,
};
pub use types::{CellData, DeviceInfo};

use protocol::{MessageIter, MessageType, RawRequest, RawResponse};
use utils::checksum;

#[cfg(feature = "android")]
uniffi::setup_scaffolding!();

// --- Public API exposed via UniFFI ---

/// Build a BMS request packet for the given command code.
/// Returns the raw bytes to write to the BLE characteristic.
#[cfg_attr(feature = "android", uniffi::export)]
pub fn build_request(command_code: u8) -> Vec<u8> {
    let req = RawRequest::from(command_code);
    let raw: &[u8] = req.as_ref();
    let mut data = raw.to_vec();
    let crc = checksum(None, &data);
    data.push(crc);
    data
}

/// Validate that a response buffer has correct CRC.
#[cfg_attr(feature = "android", uniffi::export)]
pub fn validate_response(data: Vec<u8>) -> bool {
    if data.len() < RESPONSE_HEADER.len() + 1 {
        return false;
    }
    let payload = &data[..data.len() - 1];
    let expected_crc = data[data.len() - 1];
    checksum(None, payload) == expected_crc
}

/// Parse raw response bytes into DeviceInfo.
/// The `data` should be the complete response (including header, excluding trailing CRC byte).
#[cfg_attr(feature = "android", uniffi::export)]
pub fn parse_device_info(data: Vec<u8>) -> Result<DeviceInfo> {
    DeviceInfo::try_from(data.as_slice())
}

/// Parse raw response bytes into CellData.
/// The `data` should be the complete response (including header, excluding trailing CRC byte).
#[cfg_attr(feature = "android", uniffi::export)]
pub fn parse_cell_data(data: Vec<u8>) -> Result<CellData> {
    CellData::try_from(data.as_slice())
}

/// Get the JK BMS service UUID string.
#[cfg_attr(feature = "android", uniffi::export)]
pub fn get_service_uuid() -> String {
    SERVICE_UUID.to_string()
}

/// Get the JK BMS characteristic UUID string.
#[cfg_attr(feature = "android", uniffi::export)]
pub fn get_characteristic_uuid() -> String {
    CHARACTERISTIC_UUID.to_string()
}

/// Get the command bytes for requesting device info.
#[cfg_attr(feature = "android", uniffi::export)]
pub fn get_device_info_request() -> Vec<u8> {
    build_request(CMD_DEVICE_INFO)
}

/// Get the command bytes for requesting cell data.
#[cfg_attr(feature = "android", uniffi::export)]
pub fn get_cell_data_request() -> Vec<u8> {
    build_request(CMD_CELL_DATA)
}

/// Accumulate BLE notification chunks into a complete response.
/// Returns Some(complete_data) when a full response is assembled, None if more data needed.
/// The returned data includes the header but excludes the trailing CRC.
#[cfg_attr(feature = "android", uniffi::export)]
pub fn process_notification(buffer: Vec<u8>, new_chunk: Vec<u8>) -> NotificationResult {
    let mut combined = buffer;
    combined.extend(&new_chunk);

    // Check if we have a complete message by looking for the next header after the first one
    if combined.len() < RESPONSE_HEADER.len() + 2 {
        return NotificationResult {
            buffer: combined,
            complete_data: None,
        };
    }

    // Check the first bytes are a valid response header
    if !combined.starts_with(&RESPONSE_HEADER) {
        // Try to find response header in the data
        if let Some(pos) = combined
            .windows(RESPONSE_HEADER.len())
            .position(|w| w == RESPONSE_HEADER)
        {
            combined = combined[pos..].to_vec();
        } else {
            return NotificationResult {
                buffer: Vec::new(),
                complete_data: None,
            };
        }
    }

    // Look for another header or heartbeat signaling end of message
    for msg in MessageIter::from(combined.as_slice()) {
        if msg.len() >= RESPONSE_HEADER.len() {
            if let Ok(res) = <&RawResponse>::try_from(msg) {
                if res.message_type() == Some(MessageType::Response) {
                    // We have what appears to be a complete message
                    // Check if more data follows (indicating message boundary)
                    let msg_end = msg.as_ptr() as usize - combined.as_ptr() as usize + msg.len();
                    if msg_end < combined.len() {
                        // More data after this message = message is complete
                        return NotificationResult {
                            buffer: combined[msg_end..].to_vec(),
                            complete_data: Some(msg.to_vec()),
                        };
                    }
                }
            }
        }
    }

    // Not yet complete - return buffer for more data
    NotificationResult {
        buffer: combined,
        complete_data: None,
    }
}

/// Result of processing a BLE notification chunk.
#[derive(Clone, Debug)]
#[cfg_attr(feature = "android", derive(uniffi::Record))]
pub struct NotificationResult {
    /// Updated buffer (pass back on next call)
    pub buffer: Vec<u8>,
    /// Complete response data if message is fully assembled, None if more chunks needed
    pub complete_data: Option<Vec<u8>>,
}
