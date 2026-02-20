/// BMS device information
#[derive(Clone, Default, Debug)]
#[cfg_attr(feature = "android", derive(uniffi::Record))]
pub struct DeviceInfo {
    /// Model name
    pub device_model: String,
    /// Hardware version
    pub hardware_version: String,
    /// Firmware version
    pub software_version: String,
    /// Time in seconds since poweron
    pub up_time: u32,
    /// Number of powerons
    pub poweron_times: u32,
    /// Device name
    pub device_name: String,
    /// Device passcode
    pub device_passcode: String,
    /// Manufacturing date
    pub manufacturing_date: String,
    /// Serial number
    pub serial_number: String,
    /// Passcode
    pub passcode: String,
    /// Userdata
    pub userdata: String,
    /// Passcode to change settings
    pub setup_passcode: String,
    /// Second userdata
    pub userdata2: String,
}

/// BMS cell data
#[derive(Clone, Default, Debug)]
#[cfg_attr(feature = "android", derive(uniffi::Record))]
pub struct CellData {
    /// Cell voltages in Volts
    pub cell_voltage: Vec<f32>,
    /// Average cell voltage in Volts
    pub average_cell_voltage: f32,
    /// Maximum voltage difference between cells in Volts
    pub delta_cell_voltage: f32,
    /// Balance current in Amperes
    pub balance_current: f32,
    /// Cell resistances in Ohms
    pub cell_resistance: Vec<f32>,
    /// Battery voltage between terminals in Volts
    pub battery_voltage: f32,
    /// Battery power in Watts
    pub battery_power: f32,
    /// Battery current in Amperes
    pub battery_current: f32,
    /// Battery temperatures in Celsius degrees
    pub battery_temperature: Vec<f32>,
    /// BMS power MOSFET temperature in Celsius degrees
    pub mosfet_temperature: f32,
    /// Remaining battery capacity in percent
    pub remain_percent: u8,
    /// Remaining battery capacity in Ah
    pub remain_capacity: f32,
    /// Nominal battery capacity in Ah
    pub nominal_capacity: f32,
    /// Number of battery cycles
    pub cycle_count: u32,
    /// Cycle battery capacity in Ah
    pub cycle_capacity: f32,
    /// Time in seconds since last poweron
    pub up_time: u32,
}
