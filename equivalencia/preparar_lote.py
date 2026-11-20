"""Elige el lote fijo de 40 fotos para la prueba de equivalencia y genera la referencia de Python."""
import csv, math, random, shutil, os, numpy as np
from gotera_python import Gotera, entrada
random.seed(42)
R = list(csv.reader(open('/tmp/barrido2.csv')))
f = lambda x: float(x) if x not in ('', 'nan') else math.nan
sel = []
def tomar(filas, n):
    for r in filas:
        if len(sel) < 40 and r not in sel and n > 0: sel.append(r); n -= 1
# 1. todos los casos poco comunes (no concluyente, ojo o roya, negativos aceptados)
tomar([r for r in R if r[6] in ('NO_CONCLUYENTE', 'OJO_DE_GALLO_O_ROYA')], 99)
tomar([r for r in R if r[0] == 'negativos_propios' and r[6] != 'IMAGEN_NO_VALIDA'], 99)
# 2. hojas cerca de las bandas de decisión (P(ojo) entre 0,2 y 0,8)
tomar(sorted([r for r in R if r[0] != 'negativos_propios' and 0.2 < f(r[4]) < 0.8], key=lambda r: abs(f(r[4]) - 0.5)), 99)
# 3. fotos que no son hoja, con las más cercanas al umbral del verificador primero
neg = sorted([r for r in R if r[0] == 'negativos_propios' and r[6] == 'IMAGEN_NO_VALIDA'], key=lambda r: abs(f(r[3]) - 0.5))
tomar(neg[:3], 3); tomar(random.sample(neg[3:], 4), 4)
# 4. casos típicos al azar hasta completar 40, mitad ojo de gallo y mitad sanas
falta = 40 - len(sel)
tomar(random.sample([r for r in R if r[0] == 'Ojo_Gallo' and r[6] == 'COMPATIBLE_OJO_DE_GALLO'], 30), falta // 2 + falta % 2)
tomar(random.sample([r for r in R if r[0] == 'Sanas' and r[6] == 'SIN_SINTOMAS'], 30), 40 - len(sel))
assert len(sel) == 40
A = '../../04_App_Android/OjoDeGallo/app/src/androidTest/assets/equivalencia'
for d in ('imagenes', 'entradas'): os.makedirs(f'{A}/{d}', exist_ok=True)
g = Gotera()
with open(f'{A}/referencia_python.csv', 'w', newline='') as fo, open('referencia_python.csv', 'w', newline='') as fo2:
    cab = ['id', 'archivo', 'clase', 'origen', 'desviacion', 'p_hoja', 'p_ojo', 'p_roya', 'resultado']
    w = csv.writer(fo); w2 = csv.writer(fo2); w.writerow(cab); w2.writerow(cab)
    for i, r in enumerate(sel, 1):
        ext = os.path.splitext(r[1])[1].lower()
        nombre = f'EQ{i:02d}{ext}'
        src = f'../../02_Dataset/{r[0]}/{r[1]}'
        shutil.copyfile(src, f'{A}/imagenes/{nombre}')
        entrada(src).astype('<f4').tofile(f'{A}/entradas/EQ{i:02d}.bin')
        a = g.analizar(src)
        fila = [f'EQ{i:02d}', nombre, r[0], r[1], a['desviacion'], a['p_hoja'], a['p_ojo'], a['p_roya'], a['resultado']]
        w.writerow(fila); w2.writerow(fila)
print('lote listo')
import collections; print(collections.Counter((r[0], r[6]) for r in sel))
