import gzip
import csv
import math
import struct
from pathlib import Path

# this file is meant to process athyg_v30-1.csv.gz and athyg_v30-2.csv.gz from
# https://github.com/astronexus/ATHYG-Database/tree/fd8fa128df2ba41282a7af1ef0e5c9c22c82f51f/data
# into a more useful form for this mod

STITCHED_PATH = Path('scripts/athyg_stitched.csv')
# OUTPUT_PATH = Path('star_catalog.bin')
OUTPUT_PATH = Path('src/main/resources/star_catalog.bin')

if not STITCHED_PATH.exists():
	print('\x1b[31m----- stitching tables... -----\x1b[0m')
	with open(STITCHED_PATH, 'wb') as stitched:
		with gzip.open('athyg_v30-1.csv.gz', 'r') as cat:
			stitched.write(cat.read())
		with gzip.open('athyg_v30-2.csv.gz', 'r') as cat:
			stitched.write(cat.read())

print('\x1b[31m----- processing catalogue... -----\x1b[0m')

class CatalogEntry(object):
	def __init__(self, name, is_proper_name, absmag, ci, spect, x, y, z):
		self.name = name
		self.is_proper_name = is_proper_name
		self.absmag = absmag
		self.ci = ci
		self.spect = spect
		self.x = x
		self.y = y
		self.z = z

		# HACK: this 1/0.9 is so that Sol is 1 Lsol
		self.Lsol = (1 / 0.9) * (10 ** (0.4 * (4.74 - self.absmag)))
		self.TK = 4600 * (1 / (0.92 *  self.ci + 1.7) + 1 / (0.92 * self.ci + 0.62))

	def display(self):
		# Tsol = 4600 * (1 / (0.92 *  self.ci + 1.7) + 1 / (0.92 * self.ci + 0.62)) / 5772
		# Lsol = 10 ** (0.4 * (4.74 - self.absmag))
		print(f"\x1b[35m\x1b[1m{self.name}\x1b[0m \x1b[34m{self.spect}\x1b[0m \x1b[37m{self.x} {self.y} {self.z}\x1b[0m \x1b[37mci:{self.ci}\x1b[0m \x1b[33m{self.Lsol:.02f}Lsol\x1b[0m \x1b[33m{self.TK:.02f}K\x1b[0m")

def process_row(row):
	if row['dist'] is None:
		return None
	if row['absmag'] is None:
		return None
	if row['ci'] is None:
		return None

	keep = False
	# close stars
	keep |= float(row['dist']) < 50
	# visibly bright stars
	keep |= float(row['mag']) < 6
	# bright stars
	keep |= float(row['absmag']) < 1
	# Prevent duplicate Sol (it's already added by the starting system layer)
	keep &= row['proper'] != 'Sol'

	if not keep:
		return None

	name = row['proper']
	proper = name is not None
	if name is None and row['bayer'] is not None and row['flam'] is not None:
		name = f"{row['flam']} {row['bayer']}"

	if name is None and row['gl'] is not None:
		name = f"Gliese {row['gl']}"
	if name is None and row['hr'] is not None:
		name = f"HR {row['hr']}"
	if name is None and row['hd'] is not None:
		name = f"HD {row['hd']}"
	if name is None and row['hip'] is not None:
		name = f"HIP {row['hip']}"
	if name is None and row['hyg'] is not None:
		name = f"HYG {row['hyg']}"
	if name is None and row['tyc'] is not None:
		name = f"TYC {row['tyc']}"

	# StellarCelestialNode needs the following properties: mass, luminosity, radius, temperature, position, age, star type
	# however, we only really know the star's luminosity, position, and color index.
	# we might be able to guess temperature information based on the color index, assuming that each star is an ideal black body radiator
	# with temperature and luminosity, we can derive the star's radius: La/Lb = (Ra^2 * Ta^4) / (Rb^2 * Tb^4)
	
	# main sequence stars have an apporoximate mass-luminosity relation: Lsol = Msol^3.5

	return CatalogEntry(name, proper, float(row['absmag']), float(row['ci']), row['spect'], float(row['x0']), float(row['y0']), float(row['z0']))

min_x = math.inf
max_x = -math.inf
min_y = math.inf
max_y = -math.inf
min_z = math.inf
max_z = -math.inf

BAR_CHARS = ['▏', '▎', '▍', '▌', '▋', '▊', '▉'];
HISTOGRAM_DISPLAY_WIDTH = 100

class Histogram(object):
	def __init__(self, title, label, min, max, bin_count):
		self.title = title
		self.label = label
		self.bins = [0 for i in range(0, bin_count)]
		self.min = min
		self.max = max
		self.data_points_total = 0
		self.data_points_total_no_outliers = 0
		self.outliers_low = []
		self.outliers_low_count = 0
		self.outliers_high = []
		self.outliers_high_count = 0

	def insert(self, data):
		self.data_points_total += 1
		t = (data - self.min) / (self.max - self.min)
		if t < 0:
			self.outliers_low_count += 1
			if len(self.outliers_low) < 10:
				self.outliers_low.append(data)
			return
		if t >= 1:
			self.outliers_high_count += 1
			if len(self.outliers_high) < 10:
				self.outliers_high.append(data)
			return
		self.data_points_total_no_outliers += 1
		self.bins[math.floor(len(self.bins) * t)] += 1
	
	def display(self):
		print(f'\x1b[31m----- {self.title} -----\x1b[0m')
		print(f'{self.data_points_total} data points in {len(self.bins)} bins, {self.outliers_low_count + self.outliers_high_count} outliers')

		bin_max = 0		
		for i in range(0, len(self.bins)):
			bin_max = max(bin_max, self.bins[i])

		vertical_cap = ''
		for i in range(0, HISTOGRAM_DISPLAY_WIDTH):
			vertical_cap += '═'
		print(f'╔{vertical_cap}╗')
		for i in range(0, len(self.bins)):
			count = self.bins[i]
			t = count / bin_max
			line = ""
			for j in range(0, HISTOGRAM_DISPLAY_WIDTH):
				vl = j / HISTOGRAM_DISPLAY_WIDTH
				vh = (j + 1) / HISTOGRAM_DISPLAY_WIDTH
				if t < vl and t < vh:
					line += ' '
				elif t >= vl and t >= vh:
					line += '█'
				else:
					t2 = math.floor((t - vl) / (vh - vl))
					line += BAR_CHARS[len(BAR_CHARS) * t2]
			bl = self.min + (self.max - self.min) * (i / len(self.bins))
			print(f'║{line}║ \x1b[34m{count}\x1b[0m: \x1b[0m{bl}\x1b[0m')
		print(f'╚{vertical_cap}╝')

bins_luminosity = Histogram("Luminosity", "Lsol", 0, 40, 256)
bins_temperature = Histogram("Temperature", "K", 2000, 20000, 256)
bins_x = Histogram("X", "pc?", -5000, 5000, 100)
bins_y = Histogram("Y", "pc?", -5000, 5000, 100)
bins_z = Histogram("Z", "pc?", -5000, 5000, 100)

DO_ANALYSIS = False

with STITCHED_PATH.open('rt') as cat:
	reader = csv.reader(cat)

	# skip column description
	columns = next(reader)

	kept_count = 0
	discarded_count = 0

	with OUTPUT_PATH.open('wb') as outfile:
		for row_index, row_raw in enumerate(reader):
			# if row_index > 1000:
			# 	break

			if row_index % 100000 == 0 and row_index > 0:
				total_count = discarded_count + kept_count
				percent = 100 * kept_count / total_count
				print(f'\x1b[32;1m{total_count}\x1b[0m [\x1b[32m{kept_count}\x1b[0m:\x1b[33m{discarded_count}\x1b[0m, \x1b[34m{percent:.02f}%\x1b[0m]')

			row = {}
			for i in range(0, len(columns)):
				cell = row_raw[i]
				row[columns[i]] = cell if cell != '' else None

			res = process_row(row)
			if res is not None:
				outfile.write(struct.pack('>fffff', res.x, res.y, res.z, res.Lsol, res.TK))
				outfile.write(res.name.encode())
				outfile.write(struct.pack('=B', 0))
				if res.spect is not None:
					outfile.write(res.spect.encode())
				outfile.write(struct.pack('=B', 0))

				if DO_ANALYSIS:
					min_x = min(min_x, res.x)
					min_y = min(min_y, res.y)
					min_z = min(min_z, res.z)
					max_x = max(max_x, res.x)
					max_y = max(max_y, res.y)
					max_z = max(max_z, res.z)

					bins_luminosity.insert(res.Lsol)
					bins_temperature.insert(res.TK)
					bins_x.insert(res.x)
					bins_y.insert(res.y)
					bins_z.insert(res.z)

				# print(row)
				if res.is_proper_name:
					# pass
					res.display()
				kept_count += 1
			else:
				discarded_count += 1
		
	print(f'\x1b[32mkept {kept_count} rows\x1b[0m, \x1b[33mdiscarded {discarded_count} rows\x1b[0m')
	if DO_ANALYSIS:
		print(f'\x1b[31mx in [{min_x}, {max_x}]\x1b[0m, \x1b[32my in [{min_y}, {max_y}]\x1b[0m, \x1b[34mz in [{min_z}, {max_z}]\x1b[0m')
		bins_luminosity.display()
		bins_temperature.display()
		bins_x.display()
		bins_y.display()
		bins_z.display()


# ================ OUTPUT FORMAT ================
# all fields are big endian

# strings are UTF-8 encoded and null-terminated
#
# str: [data: *u8]

# xyz are in parsecs
# luminosity is in solar luminosities (3.827e26 W)
# temperature is in kelvin
#
# entry: [x: f32] [y: f32] [z: f32] [luminosity: f32] [temperature: f32] [name: str] [classification: str]

# 
#
# file: [entries: *entry]
