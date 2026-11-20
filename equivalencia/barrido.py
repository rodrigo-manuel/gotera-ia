import glob, csv, sys, os
from gotera_python import Gotera
g = Gotera(); B = '../../02_Dataset/'
fs = [(c, f) for c in ('Ojo_Gallo', 'Sanas', 'negativos_propios') for f in sorted(glob.glob(B + c + '/*'))]
out = '/tmp/barrido2.csv'; hechos = set()
if os.path.exists(out): hechos = {l.split(',')[1] for l in open(out)}
w = csv.writer(open(out, 'a', newline=''))
n = 0
for c, f in fs:
    nom = f.split('/')[-1]
    if nom in hechos: continue
    r = g.analizar(f); w.writerow([c, nom, r['desviacion'], r['p_hoja'], r['p_ojo'], r['p_roya'], r['resultado']]); n += 1
    if n >= int(sys.argv[1]): break
print('hechos', len(hechos) + n, 'de', len(fs))
