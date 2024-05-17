import math
import struct
import re
import itertools
import tarfile
import requests
from pathlib import Path

# this file is meant to process athyg_v30-1.csv.gz and athyg_v30-2.csv.gz from
# https://github.com/astronexus/ATHYG-Database/tree/fd8fa128df2ba41282a7af1ef0e5c9c22c82f51f/data
# into a more useful form for this mod

INPUT_PATH = Path('scripts/MIST_v1.2_feh_p0.00_afe_p0.0_vvcrit0.4_basic.iso')
OUTPUT_PATH = Path('src/main/resources/star_evolution_tracks.bin')

CACHE_PATH = Path('scripts/intermediate/EEP_tracks/cache')
UNCOMPRESSED_PATH = Path('scripts/intermediate/EEP_tracks')

COLUMNS = ["EEP", "log10_isochrone_age_yr", "initial_mass", "star_mass", "star_mdot", "he_core_mass", "c_core_mass", "log_L", "log_LH", "log_LHe", "log_Teff", "log_R", "log_g", "surface_h1", "surface_he3", "surface_he4", "surface_c12", "surface_o16", "log_center_T", "log_center_Rho", "center_gamma", "center_h1", "center_he4", "center_c12", "phase"]

# extract EEP track tar files n stuff

def uncompressed_table_path(version, afe, vvcrit, feh, mass):
	feh_sign = "p" if feh >= 0 else "m"
	afe_sign = "p" if afe >= 0 else "m"
	feh = abs(feh)
	afe = abs(afe)
	return UNCOMPRESSED_PATH / Path(f"v{version}/afe_{afe_sign}{afe:.1f}/vvcrit_{vvcrit:.1f}/feh_{feh_sign}{feh:.2f}/mass_{mass:.2f}.track.eep")

def table_locations(version, feh, afe, vvcrit):
	feh_sign = "p" if feh >= 0 else "m"
	afe_sign = "p" if afe >= 0 else "m"
	feh = abs(feh)
	afe = abs(afe)
	url = f"https://waps.cfa.harvard.edu/MIST/data/tarballs_v{version}/MIST_v{version}_feh_{feh_sign}{feh:.2f}_afe_{afe_sign}{afe:.1f}_vvcrit{vvcrit:.1f}_EEPS.txz"
	cached_path = CACHE_PATH / Path(f"MIST_v{version}_feh_{feh_sign}{feh:.2f}_afe_{afe_sign}{afe:.1f}_vvcrit{vvcrit:.1f}_EEPS.txz")
	output_path = UNCOMPRESSED_PATH / Path(f"v{version}/afe_{afe_sign}{afe:.1f}/vvcrit_{vvcrit:.1f}/feh_{feh_sign}{feh:.2f}")
	return url, cached_path, output_path

MIST_VERSION = "1.2"
# FEHS = [-4.00, -3.50, -3.00, -2.50, -2.00, -1.75, -1.50, -1.25, -1.00, -0.75, -0.50, -0.25, 0.00, 0.25, 0.50]
FEHS = [-4.00, -2.00, -1.00, -0.50, 0.00, 0.50]
MASSES = [0.10,0.15,0.20,0.25,0.30,0.31,0.32,0.33,0.34,0.35,0.36,0.37,0.38,0.39,0.40,0.45,0.50,0.55,0.60,0.65,0.70,0.75,
        0.80,0.85,0.90,0.92,0.94,0.96,0.98,1.00,1.02,1.04,1.06,1.08,1.10,1.12,1.14,1.16,1.18,1.20,1.22,1.24,1.26,1.28,
		1.30,1.32,1.34,1.36,1.38,1.40,1.42,1.44,1.46,1.48,1.50,1.52,1.54,1.56,1.58,1.60,1.62,1.64,1.66,1.68,1.70,1.72,
		1.74,1.76,1.78,1.80,1.82,1.84,1.86,1.88,1.90,1.92,1.94,1.96,1.98,2.00,2.02,2.04,2.06,2.08,2.10,2.12,2.14,2.16,
		2.18,2.20,2.22,2.24,2.26,2.28,2.30,2.32,2.34,2.36,2.38,2.40,2.42,2.44,2.46,2.48,2.50,2.52,2.54,2.56,2.58,2.60,
		2.62,2.64,2.66,2.68,2.70,2.72,2.74,2.76,2.78,2.80,3.00,3.20,3.40,3.60,3.80,4.00,4.20,4.40,4.60,4.80,5.00,5.20,
		5.40,5.60,5.80,6.00,6.20,6.40,6.60,6.80,7.00,7.20,7.40,7.60,7.80,8.00,9.00,10.00,11.00,12.00,13.00,14.00,15.00,
		16.00,17.00,18.00,19.00,20.00,22.00,24.00,26.00,28.00,30.00,32.00,34.00,36.00,38.00,40.00,45.00,50.00,55.00,
		60.00,65.00,70.00,75.00,80.00,85.00,90.00,95.00,100.00,105.00,110.00,115.00,120.00,125.00,130.00,135.00,140.00,
		145.00,150.00,175.00,200.00,225.00,250.00,275.00,300.00]
AFE = 0.0
V_VCRIT = 0.4

for feh in FEHS:
	url, download_path, output_path = table_locations(MIST_VERSION, feh, AFE, V_VCRIT)
	if not download_path.exists():
		print(f"\x1b[31mDownloading \x1b[90m'{url}'\x1b[0m")
		with download_path.open('wb') as out_file:
			out_file.write(requests.get(url, stream=True).content)
	if output_path.exists():
		continue
	output_path.mkdir(parents=True)
	print(f"\x1b[32mDecompressing \x1b[90m'{download_path}'\x1b[0m")
	with tarfile.open(download_path, 'r:xz') as tar:
		for member in tar.getmembers():
			member_path = Path(*Path(member.name).parts[1:])
			if not member_path.suffix.endswith("eep"):
				continue
			mass = int(member_path.stem[:5]) / 100
			uncompressed_path = output_path / f"mass_{mass:.2f}{''.join(member_path.suffixes)}"
			print(f"\x1b[31mWriting \x1b[90m'{uncompressed_path}'\x1b[0m")
			with open(uncompressed_path, 'wb') as out_file:
				out_file.write(tar.extractfile(member).read())

TRACK_COLUMNS = ['star_age', 'star_mass', 'star_mdot', 'he_core_mass', 'c_core_mass', 'o_core_mass', 'log_L', 'log_L_div_Ledd',
				 'log_LH', 'log_LHe', 'log_LZ', 'log_Teff', 'log_abs_Lgrav', 'log_R', 'log_g', 'log_surf_z', 'surf_avg_omega',
				 'surf_avg_v_rot', 'surf_num_c12_div_num_o16', 'v_wind_Km_per_s', 'surf_avg_omega_crit', 'surf_avg_omega_div_omega_crit',
				 'surf_avg_v_crit', 'surf_avg_v_div_v_crit', 'surf_avg_Lrad_div_Ledd', 'v_div_csound_surf', 'surface_h1', 'surface_he3',
				 'surface_he4', 'surface_li7', 'surface_be9', 'surface_b11', 'surface_c12', 'surface_c13', 'surface_n14', 'surface_o16', 
				 'surface_f19', 'surface_ne20', 'surface_na23', 'surface_mg24', 'surface_si28', 'surface_s32', 'surface_ca40', 'surface_ti48',
				 'surface_fe56', 'log_center_T', 'log_center_Rho', 'center_degeneracy', 'center_omega', 'center_gamma', 'mass_conv_core',
				 'center_h1', 'center_he4', 'center_c12', 'center_n14', 'center_o16', 'center_ne20', 'center_mg24', 'center_si28', 'pp',
				 'cno', 'tri_alfa', 'burn_c', 'burn_n', 'burn_o', 'c12_c12', 'delta_nu', 'delta_Pg', 'nu_max', 'acoustic_cutoff',
				 'max_conv_vel_div_csound', 'max_gradT_div_grada', 'gradT_excess_alpha', 'min_Pgas_div_P', 'max_L_rad_div_Ledd',
				 'e_thermal', 'phase']

AGE_INDEX = TRACK_COLUMNS.index('star_age')
MASS_INDEX = TRACK_COLUMNS.index('star_mass')
LUMINOSITY_INDEX = TRACK_COLUMNS.index('log_L')
TEMPERATURE_INDEX = TRACK_COLUMNS.index('log_Teff')
RADIUS_INDEX = TRACK_COLUMNS.index('log_R')
PHASE_INDEX = TRACK_COLUMNS.index('phase')

WANTED_COLUMNS = [AGE_INDEX, MASS_INDEX, LUMINOSITY_INDEX, TEMPERATURE_INDEX, RADIUS_INDEX, PHASE_INDEX]

class BackWriter(object):
	def __init__(self, writer, pos, kind):
		self.writer = writer
		# where to emit its output at
		self.pos = pos
		# the kind of output to emit
		self.kind = kind

	def emit(self, value):
		current_pos = self.writer.output.tell()
		self.writer.output.seek(self.pos)
		if self.kind == "i8": self.writer.output.write(struct.pack('>b', value))
		elif self.kind == "u8": self.writer.output.write(struct.pack('>B', value))
		elif self.kind == "i16": self.writer.output.write(struct.pack('>h', value))
		elif self.kind == "u16": self.writer.output.write(struct.pack('>H', value))
		elif self.kind == "i32": self.writer.output.write(struct.pack('>i', value))
		elif self.kind == "u32": self.writer.output.write(struct.pack('>I', value))
		elif self.kind == "i64": self.writer.output.write(struct.pack('>q', value))
		elif self.kind == "u64": self.writer.output.write(struct.pack('>Q', value))
		elif self.kind == "f32": self.writer.output.write(struct.pack('>f', value))
		elif self.kind == "f64": self.writer.output.write(struct.pack('>d', value))
		self.writer.output.seek(current_pos)

	def link(self):
		current_pos = self.writer.output.tell()
		if self.kind == 'ptr_abs_u32':
			self.writer.output.seek(self.pos)
			self.writer.output.write(struct.pack('>I', current_pos))
		elif self.kind == 'ptr_rel_u16':
			delta = current_pos - self.pos
			assert delta <= 65535
			self.writer.output.seek(self.pos)
			self.writer.emit_u16(delta)
		else:
			assert False, f"cannot link non-pointer type {self.kind}"
		self.writer.output.seek(current_pos)

class BinaryFileWriter(object):
	def __init__(self, output):
		self.output = output

	def emit_pointer(self, kind):
		pos = self.output.tell()
		if kind == 'ptr_abs_u32':
			self.output.write(struct.pack('>I', 0xfafbfcfd))
		elif kind == 'ptr_rel_u16':
			self.output.write(struct.pack('>H', 0xfafa))
		elif kind == 'u16':
			self.output.write(struct.pack('>H', 0xfafb))
		return BackWriter(self, pos, kind)

	def emit_i8(self, value):
		self.output.write(struct.pack('>b', value))
	def emit_u8(self, value):
		self.output.write(struct.pack('>B', value))
	def emit_i16(self, value):
		self.output.write(struct.pack('>h', value))
	def emit_u16(self, value):
		self.output.write(struct.pack('>H', value))
	def emit_i32(self, value):
		self.output.write(struct.pack('>i', value))
	def emit_u32(self, value):
		self.output.write(struct.pack('>I', value))
	def emit_i64(self, value):
		self.output.write(struct.pack('>q', value))
	def emit_u64(self, value):
		self.output.write(struct.pack('>Q', value))
	def emit_f32(self, value):
		self.output.write(struct.pack('>f', value))
	def emit_f64(self, value):
		self.output.write(struct.pack('>d', value))

	def emit_bytes(self, value, encoding="utf8"):
		if isinstance(value, str):
			value = value.encode(encoding=encoding)
		self.output.write(value)

print('\x1b[31m----- processing stellar evolution tracks... -----\x1b[0m')
with OUTPUT_PATH.open('wb') as out_file:
	writer = BinaryFileWriter(out_file)

	# write metallicities to file
	writer.emit_u32(len(FEHS))
	for feh in FEHS:
		# mass param doesnt matter, the Zinit header value is the same for all track files for a given [Fe/H]
		table_path = uncompressed_table_path(MIST_VERSION, AFE, V_VCRIT, feh, 0.1)
		with table_path.open('r') as table_file:
			parse_metallicity_line = False
			metallicity = None
			eeps = None
			for i, line in enumerate(table_file):
				if i > 12:
					break
				if parse_metallicity_line:
					# Yinit, Zinit, [Fe/H], [a/Fe], v/vcrit
					cols = [float(c) for c in line[1:].strip().split()]
					metallicity = cols[1]
					break
				parse_metallicity_line = 'Yinit' in line
			writer.emit_f32(metallicity)

	# write masses to file
	writer.emit_u32(len(MASSES))
	for mass in MASSES:
		writer.emit_f32(mass)

	# actually output the table
	track_pointers = []
	for i in range(len(FEHS)):
		inner = []
		for j in range(len(MASSES)):
			inner.append(writer.emit_pointer('ptr_abs_u32'))
		track_pointers.append(inner)

	for ifeh, feh in enumerate(FEHS):
		for imass, mass in enumerate(MASSES):
			track_pointers[ifeh][imass].link()
			table_path = uncompressed_table_path(MIST_VERSION, AFE, V_VCRIT, feh, mass)
			print(f"\x1b[32m[Fe/H]\x1b[0m = {feh}, \x1b[32mmass\x1b[0m = {mass}")
			with table_path.open('r') as table_file:
				parse_metallicity_line = False
				metallicity = None
				eeps = None
				length_writer = writer.emit_pointer('u16')
				entries = 0
				for i, line in enumerate(table_file):
					# if i > 15:
					# 	break
					if line.startswith('#'):
						if parse_metallicity_line:
							# Yinit, Zinit, [Fe/H], [a/Fe], v/vcrit
							cols = [float(c) for c in line[1:].strip().split()]
							metallicity = cols[1]
						parse_metallicity_line = 'Yinit' in line
						if 'EEPs:' in line:
							eeps = [int(eep) for eep in line[7:].strip().split()]
						continue
					cols = [float(value) for value in line.strip().split()]
					cols = [cols[ix] for ix in WANTED_COLUMNS]

					# TODO: process track
					# WANTED_COLUMNS = [AGE_INDEX, MASS_INDEX, LUMINOSITY_INDEX, TEMPERATURE_INDEX, RADIUS_INDEX, PHASE_INDEX]
					# track: list({ age: f32, mass: f32, luminosity: f32, temperature: u16, radius: f32, phase: i8 })
					writer.emit_f32(cols[0])
					writer.emit_f32(cols[1])
					writer.emit_f32(10**cols[2])
					writer.emit_f32(10**cols[3])
					writer.emit_f32(10**cols[4])
					writer.emit_i8(int(cols[5]))

					entries += 1

				length_writer.emit(entries)

# """

# list(T): { count: u32, data: T[count] }

# track: list({ age: f32, mass: f32, luminosity: f32, temperature: u16, radius: f32, phase: i8 })

# @file: {
# 	metallicities: list(f32),
# 	masses: list(f32),
# 	track_pointers: u32[masses.count][metallicities.count],
# 	tracks: track[],
# }

# """
